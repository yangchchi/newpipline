# 技术规格（TS）：spec-1776821351272

## 元信息

- 流水线：AI焦虑缓解器-生产流水线
- 规格 ID：spec-1776821351272
- 关联 PRD：prd-1776776380557
- 状态：approved
- 更新时间：2026-05-11T02:08:42.921Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Node.js + TypeScript, Express/Fastify
- 前端：React + TypeScript, Next.js (可选)
- 数据库：PostgreSQL (主数据), Redis (缓存/消息队列)
- 中间件：Zod (运行时验证), Bull/Agenda (任务队列), 向量数据库 (如 Pinecone/Chroma)

## 2. 数据模型设计

```typescript
// domain/user.ts
export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole; // 'employee' | 'ai_expert' | 'content_operator' | 'admin'
  createdAt: Date;
}

// domain/tool.ts
export interface Tool {
  id: string;
  name: string;
  description: string;
  scenarios: string[];
  tags: string[];
  isActive: boolean;
  createdAt: Date;
}

// domain/demand.ts
export interface Demand {
  id: string;
  title: string;
  description: string;
  businessValue?: string;
  expectedCompletionTime?: Date;
  submitterEmail: string;
  submitterId: string;
  status: DemandStatus; // 'pending_review' | 'open' | 'in_progress' | 'completed' | 'cancelled'
  claimedByExpertId?: string;
  claimedAt?: Date;
  createdAt: Date;
  reviewedAt?: Date;
  reviewerId?: string;
}

// domain/knowledge.ts
export interface KnowledgeDocument {
  id: string;
  originalFileName: string;
  storagePath: string;
  fileType: 'pdf' | 'docx' | 'txt';
  status: 'processing' | 'available' | 'failed';
  processingError?: string;
  uploadedBy: string;
  uploadedAt: Date;
  processedAt?: Date;
  // 向量化后的文档片段元数据 (示例)
  chunks?: Array<{
    id: string;
    content: string;
    vectorId?: string;
    metadata: Record<string, any>;
  }>;
}
```

## 3. API设计

### 3.1 搜索工具
- 路径：`GET /api/v1/tools/search?keyword=:keyword`
- 请求：`keyword: string`
- 响应：
```typescript
interface ToolSearchResponse {
  tools: Array<{
    id: string;
    name: string;
    description: string;
    tags: string[];
  }>;
  total: number;
}
```
- 错误码：`200` OK, `400` 参数错误, `500` 服务器错误

### 3.2 智能问答
- 路径：`POST /api/v1/qa/ask`
- 请求：
```typescript
interface AskRequest {
  question: string;
  sessionId?: string; // 用于多轮对话上下文
}
```
- 响应：
```typescript
interface AskResponse {
  answer: string;
  references: Array<{
    documentName: string;
    chunkId?: string;
  }>;
  sessionId: string;
}
```
- 错误码：`200` OK, `400` 问题为空, `429` 请求频率限制, `500` RAG服务错误

### 3.3 提交需求
- 路径：`POST /api/v1/demands`
- 请求：
```typescript
interface CreateDemandRequest {
  title: string;
  description: string;
  businessValue?: string;
  expectedCompletionTime?: string; // ISO 8601
  contactEmail: string;
}
```
- 响应：
```typescript
interface CreateDemandResponse {
  demandId: string;
  status: DemandStatus;
  createdAt: string;
}
```
- 错误码：`201` 创建成功, `400` 验证失败, `401` 未登录, `500` 服务器错误

### 3.4 认领需求
- 路径：`POST /api/v1/demands/:demandId/claim`
- 请求：`{}`
- 响应：
```typescript
interface ClaimDemandResponse {
  demandId: string;
  newStatus: DemandStatus;
  claimedBy: string;
  claimedAt: string;
}
```
- 错误码：`200` 认领成功, `400` 需求状态不可认领, `401` 未登录, `403` 非专家角色, `404` 需求不存在, `409` 已被他人认领, `500` 服务器错误

### 3.5 上传知识库文档
- 路径：`POST /api/v1/knowledge/documents`
- 请求：`multipart/form-data` (file: File)
- 响应：
```typescript
interface UploadDocumentResponse {
  documentId: string;
  fileName: string;
  status: KnowledgeDocument['status'];
  uploadedAt: string;
}
```
- 错误码：`202` 已接受处理, `400` 文件格式/大小无效, `401` 未登录, `403` 非运营角色, `413` 文件过大, `500` 上传失败

## 4. 异常处理
- 错误规则：
  - 所有API错误响应格式：`{ code: string; message: string; details?: any }`
  - 输入验证错误：`400` + Zod解析详情
  - 认证错误：`401` + `WWW-Authenticate`头
  - 授权错误：`403` + 所需角色列表
  - 资源不存在：`404`
  - 业务规则冲突：`409`
  - 文件大小超限：`413`
  - 请求频率超限：`429`
  - 服务器错误：`500` + 内部错误ID（用于日志追踪）

## 5. 测试用例

### 5.1 工具搜索
- 输入：`keyword: "图像生成"`
- 输出：
```typescript
{
  tools: [{
    id: "tool_001",
    name: "Midjourney",
    description: "AI图像生成工具，适用于创意设计",
    tags: ["AI", "图像", "设计"]
  }],
  total: 1
}
```

### 5.2 需求提交验证
- 输入：`{ title: "", description: "测试", contactEmail: "user@company.com" }`
- 输出：`400` + `{ code: "VALIDATION_ERROR", message: "请填写[需求标题]" }`

### 5.3 需求认领冲突
- 输入：`POST /demands/demand_001/claim` (需求状态为`in_progress`)
- 输出：`409` + `{ code: "DEMAND_ALREADY_CLAIMED", message: "该需求已被认领" }`

### 5.4 知识库文档上传
- 输入：`file: example.pdf` (size: 60MB)
- 输出：`413` + `{ code: "FILE_TOO_LARGE", message: "文件大小超过50MB限制" }`

### 5.5 RAG问答无结果
- 输入：`{ question: "公司明年战略是什么？" }` (知识库无相关信息)
- 输出：
```typescript
{
  answer: "知识库中暂无此问题答案，您可以将此问题转为「需求」提交，或通过反馈入口联系我们。",
  references: [],
  sessionId: "sess_123"
}
```

---

## Machine-Readable JSON（附录）

```json

```
