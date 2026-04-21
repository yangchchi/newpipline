# 技术规格（TS）：spec-1776759828531

## 元信息

- 流水线：带AI功能的个人笔记-生产流水线
- 规格 ID：spec-1776759828531
- 关联 PRD：prd-1776753884369
- 状态：approved
- 更新时间：2026-04-21T09:23:31.923Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Node.js (运行时)， Express (HTTP框架)
- 前端：React (UI框架)， Zustand (状态管理)
- 数据库：SQLite (本地)， PostgreSQL (服务器)
- 中间件：LocalForage (本地存储)， PouchDB (离线同步)

## 2. 数据模型设计
### domain/Note.ts
```typescript
export interface Note {
  id: string;
  title: string;
  content: string;
  folderId: string | null;
  tags: string[];
  isAiGenerated: boolean; // 全局AI标识
  createdAt: Date;
  updatedAt: Date;
  deletedAt: Date | null;
  syncStatus: 'synced' | 'pending' | 'conflict';
  version: number;
}

export interface NoteParagraph {
  id: string;
  noteId: string;
  content: string;
  isAiGenerated: boolean; // 段落级AI标识
  order: number;
}
```

### domain/Folder.ts
```typescript
export interface Folder {
  id: string;
  name: string;
  parentId: string | null;
  userId: string;
}
```

### domain/AiSuggestion.ts
```typescript
export interface AiSuggestion {
  type: 'tag' | 'summary' | 'folder' | 'writing' | 'grammar';
  originalText?: string;
  suggestedText: string;
  confidence: number;
  position?: { start: number; end: number }; // 用于语法修正定位
}
```

## 3. API设计
### 3.1 智能整理API
- 路径：`POST /api/v1/notes/:noteId/intelligent-organize`
- 请求：
  ```typescript
  interface IntelligentOrganizeRequest {
    content: string;
  }
  ```
- 响应：
  ```typescript
  interface IntelligentOrganizeResponse {
    tags: string[];
    summary: string;
    folderSuggestions: Array<{ id: string; name: string; confidence: number }>;
  }
  ```
- 错误码：
  - 400: 内容字符数≤10
  - 408: AI服务超时(>3秒)
  - 503: AI服务不可用

### 3.2 内容辅助API
- 路径：`POST /api/v1/notes/assistance`
- 请求：
  ```typescript
  interface ContentAssistanceRequest {
    type: 'writing' | 'grammar';
    text: string;
    noteId?: string;
  }
  ```
- 响应：
  ```typescript
  interface ContentAssistanceResponse {
    suggestions: AiSuggestion[];
  }
  ```
- 错误码：
  - 400: 文本字符数≤10
  - 503: 服务降级

### 3.3 智能检索API
- 路径：`POST /api/v1/notes/search`
- 请求：
  ```typescript
  interface SemanticSearchRequest {
    query: string;
    limit?: number;
  }
  ```
- 响应：
  ```typescript
  interface SemanticSearchResponse {
    results: Array<{
      noteId: string;
      title: string;
      snippet: string;
      folderName: string;
      updatedAt: Date;
      relevance: number;
    }>;
  }
  ```

### 3.4 离线同步API
- 路径：`POST /api/v1/sync/push`
- 请求：
  ```typescript
  interface SyncPushRequest {
    changes: Array<{
      noteId: string;
      version: number;
      operation: 'create' | 'update' | 'delete';
      data?: Partial<Note>;
    }>;
  }
  ```
- 响应：
  ```typescript
  interface SyncPushResponse {
    conflicts: Array<{
      noteId: string;
      localVersion: number;
      serverVersion: number;
      serverData: Note;
    }>;
    syncedNoteIds: string[];
  }
  ```

## 4. 异常处理
- 错误规则：
  1. AI服务超时(>3秒)：降级为仅保存笔记，显示Toast
  2. 网络断开：禁用AI功能，启用离线队列
  3. 同步冲突：触发合并流程，保留双方版本
  4. 输入无效(字符数≤10)：前端拦截，不调用后端

## 5. 测试用例
### 5.1 智能整理服务
- 输入：
  ```typescript
  const request = {
    content: "TypeScript 5.0引入了新的装饰器提案，显著改进了元编程能力..."
  };
  ```
- 输出：
  ```typescript
  const response = {
    tags: ["TypeScript", "装饰器", "元编程"],
    summary: "TypeScript 5.0的新装饰器提案增强了元编程功能...",
    folderSuggestions: [
      { id: "tech", name: "技术笔记", confidence: 0.9 }
    ]
  };
  ```

### 5.2 语法修正服务
- 输入：
  ```typescript
  const request = {
    type: "grammar",
    text: "He go to school everyday."
  };
  ```
- 输出：
  ```typescript
  const response = {
    suggestions: [{
      type: "grammar",
      originalText: "go",
      suggestedText: "goes",
      confidence: 0.95,
      position: { start: 3, end: 5 }
    }]
  };
  ```

### 5.3 离线同步冲突
- 输入：
  ```typescript
  const localChange = { noteId: "1", version: 2, operation: "update" };
  const serverData = { id: "1", version: 3, title: "不同标题" };
  ```
- 输出：
  ```typescript
  const conflict = {
    noteId: "1",
    localVersion: 2,
    serverVersion: 3,
    serverData: { id: "1", version: 3, title: "不同标题" }
  };
  ```

### 5.4 边界测试
- 输入：`{ content: "hi" }` (字符数≤10)
- 输出：HTTP 400错误，前端显示"内容过短"

---
**实现注释**：
- 所有DTO使用Zod验证：`dto/IntelligentOrganizeRequest.ts`
- AI服务客户端：`infra/AiServiceClient.ts`，包含超时和重试逻辑
- 加密层：`infra/CryptoService.ts`，使用Web Crypto API
- 同步队列：`infra/SyncQueue.ts`，基于PouchDB实现

---

## Machine-Readable JSON（附录）

```json

```
