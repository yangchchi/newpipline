# 技术规格（TS）：spec-1776257491363

## 元信息

- 流水线：创建带AI分析一个记事本-生产流水线
- 规格 ID：spec-1776257491363
- 关联 PRD：prd-1776257362497
- 状态：approved
- 更新时间：2026-04-15T12:56:00.824Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 语言：TypeScript 5+
- 框架：Node.js + Express / NestJS (选择其一)

## 2. 数据模型
```typescript
// domain/models.ts
namespace domain {
    export type UserRole = 'admin' | 'member' | 'guest';
    export type TaskStatus = 'pending' | 'confirmed' | 'done' | 'cancelled';

    export interface User {
        id: string;
        role: UserRole;
    }

    export interface Note {
        id: string;
        ownerId: string;
        content: string;
        title: string;
        permissions: NotePermission[];
        metadata: NoteMetadata;
    }

    export interface NotePermission {
        userId: string;
        canView: boolean;
        canEdit: boolean;
    }

    export interface NoteMetadata {
        summary?: string;
        tasks: Task[];
        lastAnalyzedAt?: Date;
    }

    export interface Task {
        id: string;
        rawText: string;
        description: string;
        status: TaskStatus;
        assignee: string | null;
        dueDate: Date | null;
        noteId: string;
    }
}
```

## 3. API

### 3.1 智能摘要生成
- 路径：`POST /api/v1/notes/:noteId/summary`
- 请求：
```typescript
// dto/requests.ts
namespace dto {
    export interface GenerateSummaryRequest {
        token: string;
        selection?: {
            start: number;
            end: number;
        };
    }
}
```
- 响应：
```typescript
// dto/responses.ts
namespace dto {
    export interface GenerateSummaryResponse {
        status: 'success' | 'error';
        summary?: string;
        code?: string;
        message?: string;
    }
}
```
- 错误码：
    - `PERMISSION_DENIED`: 权限不足
    - `NOTE_NOT_FOUND`: 笔记不存在或无权访问
    - `CONTENT_TOO_SHORT`: 内容过短，无法生成摘要
    - `AI_SERVICE_UNAVAILABLE`: AI服务暂时不可用
    - `CONTENT_VIOLATION`: 内容不符合使用规范
    - `RATE_LIMIT_EXCEEDED`: 请求过于频繁

### 3.2 智能问答
- 路径：`POST /api/v1/notes/:noteId/qa`
- 请求：
```typescript
namespace dto {
    export interface AskQuestionRequest {
        token: string;
        question: string;
    }
}
```
- 响应：
```typescript
namespace dto {
    export interface AskQuestionResponse {
        status: 'success' | 'error';
        answer?: string;
        source?: string;
        code?: string;
        message?: string;
    }
}
```
- 错误码：同3.1

### 3.3 获取任务列表
- 路径：`GET /api/v1/notes/:noteId/tasks`
- 请求：`token` in header
- 响应：
```typescript
namespace dto {
    export interface GetTasksResponse {
        status: 'success' | 'error';
        tasks?: domain.Task[];
        code?: string;
        message?: string;
    }
}
```

## 4. 核心流程

### 4.1 智能摘要生成
```typescript
// application/summaryService.ts
namespace application {
    export class SummaryService {
        constructor(
            private authService: infra.AuthService,
            private noteRepository: infra.NoteRepository,
            private aiService: infra.AIService,
            private rateLimiter: infra.RateLimiter
        ) {}

        async generateSummary(
            noteId: string,
            request: dto.GenerateSummaryRequest
        ): Promise<dto.GenerateSummaryResponse> {
            // 1. 权限与频率检查
            const user = await this.authService.verifyToken(request.token);
            if (user.role === 'guest') {
                return { status: 'error', code: 'PERMISSION_DENIED' };
            }

            if (!this.rateLimiter.check('summary', user.id)) {
                return { status: 'error', code: 'RATE_LIMIT_EXCEEDED' };
            }

            // 2. 获取笔记内容
            const note = await this.noteRepository.findById(noteId);
            if (!note || !this.hasViewPermission(note, user.id)) {
                return { status: 'error', code: 'NOTE_NOT_FOUND' };
            }

            // 3. 提取待分析文本
            let content = note.content;
            if (request.selection) {
                content = content.substring(request.selection.start, request.selection.end);
            }

            if (content.length < 10) {
                return { status: 'error', code: 'CONTENT_TOO_SHORT' };
            }

            // 4. 调用AI服务
            try {
                const summary = await this.aiService.generateSummary(content);
                if (this.aiService.isContentViolation(summary)) {
                    return { status: 'error', code: 'CONTENT_VIOLATION' };
                }
                return { status: 'success', summary };
            } catch (error) {
                if (error instanceof infra.AITimeoutError) {
                    return { status: 'error', code: 'AI_SERVICE_UNAVAILABLE' };
                }
                throw error;
            }
        }

        private hasViewPermission(note: domain.Note, userId: string): boolean {
            return note.permissions.some(p => p.userId === userId && p.canView);
        }
    }
}
```

### 4.2 任务识别（异步）
```typescript
// application/taskService.ts
namespace application {
    export class TaskService {
        constructor(
            private aiService: infra.AIService,
            private noteRepository: infra.NoteRepository,
            private eventBus: infra.EventBus
        ) {}

        async processNoteForTasks(noteId: string): Promise<void> {
            const note = await this.noteRepository.findById(noteId);
            if (!note) return;

            const tasks = await this.aiService.extractTasks(note.content);
            
            const domainTasks: domain.Task[] = tasks.map((task, index) => ({
                id: `task_${noteId}_${index}`,
                rawText: task.rawText,
                description: task.description,
                status: 'pending',
                assignee: null,
                dueDate: null,
                noteId: note.id
            }));

            note.metadata.tasks = domainTasks;
            note.metadata.lastAnalyzedAt = new Date();
            
            await this.noteRepository.update(note);
            this.eventBus.publish('tasks.updated', { noteId, tasks: domainTasks });
        }
    }
}
```

### 4.3 智能问答
```typescript
// application/qaService.ts
namespace application {
    export class QAService {
        constructor(
            private authService: infra.AuthService,
            private noteRepository: infra.NoteRepository,
            private aiService: infra.AIService,
            private rateLimiter: infra.RateLimiter
        ) {}

        async askQuestion(
            noteId: string,
            request: dto.AskQuestionRequest
        ): Promise<dto.AskQuestionResponse> {
            // 权限与频率检查（同摘要服务）
            if (request.question.length < 2) {
                return { status: 'error', code: 'QUESTION_TOO_SHORT' };
            }

            const note = await this.noteRepository.findById(noteId);
            // 权限检查（同摘要服务）

            try {
                const result = await this.aiService.answerQuestion(note.content, request.question);
                
                if (!result.hasAnswer) {
                    return { 
                        status: 'success', 
                        answer: '根据现有笔记，无法回答此问题。' 
                    };
                }

                return {
                    status: 'success',
                    answer: result.answer,
                    source: result.source
                };
            } catch (error) {
                // 错误处理（同摘要服务）
            }
        }
    }
}
```

## 5. 异常处理
- 错误规则：
    1. 权限错误：HTTP 403 + `PERMISSION_DENIED`
    2. 资源不存在：HTTP 404 + `NOTE_NOT_FOUND`
    3. 输入验证错误：HTTP 400 + `CONTENT_TOO_SHORT`/`QUESTION_TOO_SHORT`
    4. AI服务错误：HTTP 503 + `AI_SERVICE_UNAVAILABLE`
    5. 内容违规：HTTP 400 + `CONTENT_VIOLATION`
    6. 频率限制：HTTP 429 + `RATE_LIMIT_EXCEEDED`
    7. 服务器内部错误：HTTP 500 + `INTERNAL_ERROR`

## 6. 测试用例

### 6.1 智能摘要生成
- 输入：
```typescript
{
    noteId: "note_123",
    request: { token: "guest_token" }
}
```
- 输出：
```typescript
{
    status: "error",
    code: "PERMISSION_DENIED",
    message: "权限不足"
}
```

- 输入：
```typescript
{
    noteId: "note_123",
    request: { 
        token: "user_token",
        selection: { start: 0, end: 5 }
    }
}
```
- 输出：
```typescript
{
    status: "error",
    code: "CONTENT_TOO_SHORT",
    message: "内容过短，无法生成摘要"
}
```

### 6.2 智能问答
- 输入：
```typescript
{
    noteId: "note_123",
    request: {
        token: "user_token",
        question: "预算多少？"
    }
}
// 笔记内容："项目预算为50万，截止日期是2024年12月31日"
```
- 输出：
```typescript
{
    status: "success",
    answer: "预算为50万。",
    source: "项目预算为50万"
}
```

### 6.3 任务识别
- 输入：
```typescript
// 保存笔记事件触发
{
    noteId: "note_456",
    content: "明天测试部完成压力测试，产品组需要在下周五前评审PRD"
}
```
- 输出：
```typescript
// 笔记元数据更新后
{
    tasks: [
        {
            id: "task_note_456_0",
            rawText: "明天测试部完成压力测试",
            description: "完成压力测试",
            status: "pending",
            assignee: null,
            dueDate: null,
            noteId: "note_456"
        },
        {
            id: "task_note_456_1",
            rawText: "产品组需要在下周五前评审PRD",
            description: "评审PRD",
            status: "pending",
            assignee: null,
            dueDate: null,
            noteId: "note_456"
        }
    ]
}
```

---

## Machine-Readable JSON（附录）

```json

```
