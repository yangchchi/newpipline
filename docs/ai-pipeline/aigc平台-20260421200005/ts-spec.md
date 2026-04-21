# 技术规格（TS）：spec-1776772246262

## 元信息

- 流水线：AIGC平台-生产流水线
- 规格 ID：spec-1776772246262
- 关联 PRD：prd-1776758224209
- 状态：draft
- 更新时间：2026-04-21T11:58:34.922Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Python 3.11+ (FastAPI)
- 前端：React 18+ (Vite + TypeScript)
- 数据库：PostgreSQL 15+
- 中间件：Redis (配额缓存/限流)

## 2. 数据模型设计
```sql
-- 用户与组织
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('user', 'dept_manager', 'admin', 'super_admin')),
    department_id UUID NOT NULL REFERENCES departments(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    monthly_token_quota INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL
);

-- 配额管理
CREATE TABLE user_token_quotas (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    total_quota INTEGER NOT NULL DEFAULT 0,
    used_quota INTEGER NOT NULL DEFAULT 0,
    last_reset_date DATE NOT NULL,
    CHECK (used_quota <= total_quota)
);

-- 模型管理
CREATE TABLE ai_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    model_type VARCHAR(32) NOT NULL CHECK (model_type IN ('text_generation', 'text_to_image')),
    provider VARCHAR(64) NOT NULL,
    api_endpoint VARCHAR(512) NOT NULL,
    api_key_encrypted TEXT NOT NULL,
    pricing_params JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'enabled' CHECK (status IN ('enabled', 'disabled', 'abnormal')),
    available_scope JSONB NOT NULL DEFAULT '{"type": "all"}'::jsonb,
    failure_count INTEGER DEFAULT 0,
    last_failure_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容生成记录
CREATE TABLE generation_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    template_type VARCHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    generated_content TEXT NOT NULL,
    input_tokens INTEGER NOT NULL,
    output_tokens INTEGER NOT NULL,
    total_cost DECIMAL(10,6) NOT NULL,
    model_id UUID NOT NULL REFERENCES ai_models(id),
    safety_status VARCHAR(16) NOT NULL DEFAULT 'passed' CHECK (safety_status IN ('passed', 'blocked')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 审计日志
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    operation_type VARCHAR(32) NOT NULL,
    resource_id UUID,
    details JSONB,
    ip_address INET,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 3. API设计
### 3.1 文案生成
- 路径：`POST /api/v1/generation/text`
- 请求：
```typescript
interface TextGenerationRequest {
    template_type: string;  // e.g., "公众号标题"
    core_theme: string;
    style?: string;
    word_count?: number;
}
```
- 响应：
```typescript
interface TextGenerationResponse {
    task_id: string;
    content: string;  // 流式返回时为分块内容
    estimated_tokens: number;
    status: 'generating' | 'completed' | 'blocked' | 'quota_exhausted';
    safety_check_passed: boolean;
}
```
- 错误码：
  - `400`: 请求参数无效
  - `403`: 无权限或配额不足
  - `429`: 频率限制
  - `503`: 无可用模型

### 3.2 配额查询
- 路径：`GET /api/v1/quota/status`
- 请求：无
- 响应：
```typescript
interface QuotaStatusResponse {
    total_quota: number;
    used_quota: number;
    remaining_quota: number;
    warning_threshold: number;  // 0.2
    last_reset_date: string;
}
```

### 3.3 部门配额管理
- 路径：`PUT /api/v1/admin/departments/{dept_id}/quota`
- 请求：
```typescript
interface DepartmentQuotaUpdateRequest {
    monthly_token_quota: number;
}
```
- 响应：
```typescript
interface DepartmentQuotaUpdateResponse {
    department_id: string;
    previous_quota: number;
    new_quota: number;
    updated_at: string;
}
```
- 错误码：
  - `400`: 配额值无效
  - `403`: 非管理员
  - `409`: 用户配额总和超过部门配额

### 3.4 模型接入
- 路径：`POST /api/v1/admin/models`
- 请求：
```typescript
interface ModelRegistrationRequest {
    name: string;
    model_type: 'text_generation' | 'text_to_image';
    provider: string;
    api_endpoint: string;
    api_key: string;
    pricing_params: {
        input_token_price: number;
        output_token_price: number;
    };
    available_scope: {
        type: 'all' | 'specific';
        tenant_ids?: string[];
    };
}
```
- 响应：
```typescript
interface ModelRegistrationResponse {
    model_id: string;
    status: 'enabled' | 'validation_failed';
    validation_message?: string;
}
```
- 错误码：
  - `400`: 参数验证失败
  - `403`: 非超级管理员
  - `422`: API端点验证失败

## 4. 异常处理
- 错误规则：
  1. 输入验证失败：返回400，错误信息包含具体字段
  2. 权限不足：返回403，日志记录用户ID和资源
  3. 配额耗尽：返回403，错误码`QUOTA_EXHAUSTED`
  4. 安全审核失败：返回400，错误码`CONTENT_BLOCKED`
  5. 模型服务异常：按R4降级规则处理，返回503
  6. 数据库异常：返回500，记录错误ID供追溯
  7. 网络超时：重试3次后标记模型异常

## 5. 测试用例
### 5.1 文案生成-正常流程
- 输入：
```json
{
    "template_type": "公众号标题",
    "core_theme": "春季新品发布",
    "style": "正式",
    "word_count": 20
}
```
- 输出：
```json
{
    "task_id": "uuid",
    "content": "【新品上市】春季焕新，共鉴非凡...",
    "estimated_tokens": 150,
    "status": "completed",
    "safety_check_passed": true
}
```

### 5.2 文案生成-配额不足
- 前置条件：用户剩余配额=5，估算消耗=100
- 输入：同上
- 输出：
```json
{
    "error": {
        "code": "QUOTA_EXHAUSTED",
        "message": "您的Token配额已用尽，无法生成新内容。请联系部门管理员申请增加配额。"
    }
}
```

### 5.3 文案生成-安全审核失败
- 前置条件：用户输入包含违规关键词
- 输入：
```json
{
    "template_type": "公众号标题",
    "core_theme": "敏感词测试",
    "style": "正式"
}
```
- 输出：
```json
{
    "error": {
        "code": "CONTENT_BLOCKED",
        "message": "内容不符合安全规范，请调整提示词。"
    }
}
```

### 5.4 部门配额更新-正常
- 前置条件：管理员角色，部门ID存在
- 输入：
```json
{
    "monthly_token_quota": 100000
}
```
- 输出：
```json
{
    "department_id": "dept-uuid",
    "previous_quota": 50000,
    "new_quota": 100000,
    "updated_at": "2024-01-15T10:30:00Z"
}
```

### 5.5 模型接入-验证失败
- 前置条件：超级管理员，无效API密钥
- 输入：
```json
{
    "name": "Test Model",
    "model_type": "text_generation",
    "provider": "OpenAI",
    "api_endpoint": "https://api.openai.com/v1/chat/completions",
    "api_key": "invalid-key",
    "pricing_params": {
        "input_token_price": 0.002,
        "output_token_price": 0.008
    },
    "available_scope": {
        "type": "all"
    }
}
```
- 输出：
```json
{
    "model_id": null,
    "status": "validation_failed",
    "validation_message": "模型接入失败，请检查端点与密钥。"
}
```

---

## Machine-Readable JSON（附录）

```json

```
