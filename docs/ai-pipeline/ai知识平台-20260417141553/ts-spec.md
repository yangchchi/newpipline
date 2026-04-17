# 技术规格（TS）：spec-1776398269439

## 元信息

- 流水线：AI知识平台-生产流水线
- 规格 ID：spec-1776398269439
- 关联 PRD：prd-1776397379463
- 状态：reviewing
- 更新时间：2026-04-17T06:15:30.914Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Node.js (TypeScript), Express.js
- 前端：React (TypeScript), Ant Design
- 数据库：PostgreSQL
- 中间件：Redis (缓存/会话)， RabbitMQ/Kafka (异步通知)

## 2. 数据模型设计
```typescript
// domain/user.ts
namespace domain {
    export enum UserRole {
        GUEST = 'guest',
        REQUESTER = 'requester',
        AI_EXPERT = 'ai_expert',
        OPERATOR = 'operator',
        ADMIN = 'admin'
    }

    export interface User {
        id: string;
        name: string;
        email: string;
        role: UserRole;
        tags: string[]; // 专家标签
        createdAt: Date;
    }
}

// domain/requirement.ts
namespace domain {
    export enum RequirementStatus {
        PENDING_RESPONSE = 'pending_response',
        IN_PROGRESS = 'in_progress',
        PENDING_ACCEPTANCE = 'pending_acceptance',
        COMPLETED = 'completed',
        CLOSED = 'closed'
    }

    export interface Requirement {
        id: string;
        title: string;
        description: string;
        category: string;
        expectedCompletionDate: Date;
        status: RequirementStatus;
        requesterId: string;
        acceptedExpertId: string | null;
        expertCandidates: string[]; // 接单的专家ID列表
        createdAt: Date;
        updatedAt: Date;
    }
}

// domain/case.ts
namespace domain {
    export enum CaseStatus {
        PENDING_REVIEW = 'pending_review',
        PUBLISHED = 'published',
        REJECTED = 'rejected'
    }

    export interface Case {
        id: string;
        title: string;
        scenario: string;
        category: string;
        tools: string[];
        steps: string; // 富文本HTML
        summary: string;
        status: CaseStatus;
        authorId: string;
        reviewerId: string | null;
        rejectReason: string | null;
        createdAt: Date;
        publishedAt: Date | null;
    }
}

// domain/knowledge.ts
namespace domain {
    export interface KnowledgeDocument {
        id: string;
        title: string;
        content: string;
        embedding: number[] | null;
        source: string;
        createdAt: Date;
    }
}

// domain/qa.ts
namespace domain {
    export interface QAFeedback {
        id: string;
        question: string;
        answer: string;
        isHelpful: boolean;
        userId: string | null;
        createdAt: Date;
    }
}
```

## 3. API设计
### 3.1 智能问答
- 路径：`POST /api/v1/qa/ask`
- 请求：
```typescript
namespace dto {
    export interface AskRequest {
        question: string;
        sessionId?: string;
    }
}
```
- 响应：`text/event-stream` (SSE)
```
event: chunk
data: {"content": "答案片段", "isFinal": false}

event: final
data: {"content": "", "sources": [{"title": "文档1", "url": "..."}]}
```
- 错误码：
    - 400: 问题为空或过长
    - 401: 未登录（可选，根据业务规则）
    - 500: 服务端错误

### 3.2 需求发布
- 路径：`POST /api/v1/requirements`
- 请求：
```typescript
namespace dto {
    export interface CreateRequirementRequest {
        title: string;
        description: string;
        category: string;
        expectedCompletionDate: string; // ISO8601
    }
}
```
- 响应：
```typescript
namespace dto {
    export interface RequirementResponse {
        id: string;
        title: string;
        description: string;
        category: string;
        expectedCompletionDate: string;
        status: domain.RequirementStatus;
        requesterId: string;
        createdAt: string;
    }
}
```
- 错误码：
    - 400: 字段验证失败
    - 401: 未登录
    - 403: 用户角色非requester

### 3.3 需求接单
- 路径：`POST /api/v1/requirements/:id/accept`
- 请求：`{}` (从会话获取用户ID)
- 响应：
```typescript
namespace dto {
    export interface AcceptRequirementResponse {
        success: boolean;
        requirementId: string;
        expertId: string;
        currentStatus: domain.RequirementStatus;
        expertCandidates: string[];
    }
}
```
- 错误码：
    - 400: 需求状态非`pending_response`
    - 401: 未登录
    - 403: 用户角色非ai_expert
    - 404: 需求不存在

### 3.4 案例发布
- 路径：`POST /api/v1/cases`
- 请求：
```typescript
namespace dto {
    export interface CreateCaseRequest {
        title: string;
        scenario: string;
        category: string;
        tools: string[];
        steps: string;
        summary: string;
    }
}
```
- 响应：
```typescript
namespace dto {
    export interface CaseResponse {
        id: string;
        title: string;
        scenario: string;
        category: string;
        status: domain.CaseStatus;
        createdAt: string;
    }
}
```
- 错误码：
    - 400: 字段验证失败
    - 401: 未登录

## 4. 异常处理
- 错误规则：
    1. 所有API错误返回标准格式：`{code: number, message: string, details?: unknown}`
    2. 输入验证使用Zod，错误转换为400
    3. 业务规则违反返回400或403
    4. 未捕获异常记录日志并返回500
    5. 外部服务（LLM API）超时（>5s）触发降级逻辑

## 5. 测试用例
### 5.1 智能问答降级逻辑
- 输入：question="如何造火箭？"，知识库检索结果为空
- 输出：`event: final` data包含`sources: []`，前端显示"未在知识库中找到相关信息。"

### 5.2 需求发布字段验证
- 输入：`{title: "", description: "test", category: "auto", expectedCompletionDate: "2023-01-01"}`
- 输出：HTTP 400，错误详情包含`title`和`expectedCompletionDate`字段错误

### 5.3 需求接单状态校验
- 输入：需求ID=456（状态为`in_progress`），用户角色=ai_expert
- 输出：HTTP 400，message="该需求已无法接单"

### 5.4 案例状态流转
- 输入：案例ID=789，运营人员执行`审核通过`操作
- 输出：案例状态从`pending_review`变为`published`，`publishedAt`设置为当前时间

---
// SDD: FS-用户故事1，FS-用户故事2，FS-用户故事3，FS-用户故事4
// 边界输入：所有DTO使用Zod schema验证
// 类型导出：domain/*, dto/* 为公共契约

---

## Machine-Readable JSON（附录）

```json

```
