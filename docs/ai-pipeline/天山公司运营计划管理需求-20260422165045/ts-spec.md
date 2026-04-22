# 技术规格（TS）：spec-1776846019145

## 元信息

- 流水线：天山公司运营计划管理需求-生产流水线
- 规格 ID：spec-1776846019145
- 关联 PRD：prd-1776845987780
- 状态：approved
- 更新时间：2026-04-22T08:50:07.016Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Node.js + TypeScript
- 前端：Next.js + Tailwind + TypeScript + ESLint + Materio Next.js Admin Template
- 数据库：PostgreSQL
- 中间件：Redis (缓存)

## 2. 数据模型设计
```sql
-- 用户表 (从人事系统同步)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    system_role VARCHAR(50) CHECK (system_role IN ('SYSTEM_ADMIN', 'PLAN_ADMIN', 'PROJECT_MANAGER', 'STAFF')),
    is_active BOOLEAN DEFAULT true,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL
);


-- 岗位表
CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


-- 模板表
CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_by UUID REFERENCES users(id) NOT NULL,
    created_department VARCHAR(100) NOT NULL,
    current_version INTEGER NOT NULL DEFAULT 1,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


-- 模板版本表
CREATE TABLE template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID REFERENCES templates(id) NOT NULL,
    version INTEGER NOT NULL,
    remark TEXT,
    data JSONB NOT NULL, -- 存储完整的模板结构
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(template_id, version)
);


-- 模板任务节点表 (用于快速查询关联)
CREATE TABLE template_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_version_id UUID REFERENCES template_versions(id) NOT NULL,
    task_id VARCHAR(50) NOT NULL, -- 模板内的任务ID
    name VARCHAR(200) NOT NULL,
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    is_kcp BOOLEAN DEFAULT false,
    position_ids UUID[] NOT NULL, -- 关联的岗位ID数组
    dependencies JSONB NOT NULL, -- [{taskId, type, lagDays}]
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


-- 项目表
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    template_id UUID REFERENCES templates(id) NOT NULL,
    template_version_id UUID REFERENCES template_versions(id) NOT NULL,
    plan_start_date DATE NOT NULL,
    status VARCHAR(50) CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')) DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


-- 项目任务表
CREATE TABLE project_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) NOT NULL,
    template_task_id UUID REFERENCES template_tasks(id) NOT NULL,
    name VARCHAR(200) NOT NULL,
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    is_kcp BOOLEAN DEFAULT false,
    plan_start_date DATE,
    plan_finish_date DATE,
    actual_start_date DATE,
    actual_finish_date DATE,
    status VARCHAR(50) CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')) DEFAULT 'NOT_STARTED',
    position_ids UUID[] NOT NULL,
    assigned_user_ids UUID[] NOT NULL, -- 分配的具体用户ID数组
    dependencies JSONB NOT NULL,
    is_on_critical_path BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


-- 项目任务依赖表 (用于快速查询)
CREATE TABLE project_task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) NOT NULL,
    from_task_id UUID REFERENCES project_tasks(id) NOT NULL,
    to_task_id UUID REFERENCES project_tasks(id) NOT NULL,
    type VARCHAR(2) CHECK (type IN ('FS', 'SS', 'FF', 'SF')) NOT NULL,
    lag_days INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## 3. API设计

### 3.1 模板管理API

#### POST /api/v1/templates
- **路径**: `/api/v1/templates`
- **请求**:
```typescript
{
  name: string;
  type: string;
  tasks: Array<{
    id: string;
    name: string;
    durationDays: number;
    isKcp: boolean;
    positionIds: string[];
    dependencies: Array<{
      taskId: string;
      type: 'FS' | 'SS' | 'FF' | 'SF';
      lagDays: number;
    }>;
  }>;
  remark?: string;
}
```
- **响应**:
```typescript
{
  id: string;
  version: number;
  createdAt: string;
}
```
- **错误码**:
  - 400: 模板校验失败
  - 409: 循环依赖检测失败

#### GET /api/v1/templates
- **路径**: `/api/v1/templates?type=&department=&page=&size=`
- **请求**: Query参数
- **响应**:
```typescript
{
  items: Array<{
    id: string;
    name: string;
    type: string;
    createdBy: string;
    createdDepartment: string;
    currentVersion: number;
    updatedAt: string;
  }>;
  total: number;
  page: number;
  size: number;
}
```

#### PUT /api/v1/templates/{id}
- **路径**: `/api/v1/templates/{id}`
- **请求**: 同POST
- **响应**:
```typescript
{
  id: string;
  version: number;
  updatedAt: string;
}
```

#### GET /api/v1/templates/{id}/versions
- **路径**: `/api/v1/templates/{id}/versions`
- **请求**: 无
- **响应**:
```typescript
Array<{
  version: number;
  remark?: string;
  createdAt: string;
  createdBy: string;
}>
```

#### POST /api/v1/templates/{id}/rollback/{version}
- **路径**: `/api/v1/templates/{id}/rollback/{version}`
- **请求**: 无
- **响应**:
```typescript
{
  id: string;
  currentVersion: number;
}
```

### 3.2 项目管理API

#### POST /api/v1/projects
- **路径**: `/api/v1/projects`
- **请求**:
```typescript
{
  name: string;
  description?: string;
  templateId: string;
  planStartDate: string; // ISO日期
  positionAssignments: Record<string, string[]>; // positionId -> userId[]
}
```
- **响应**:
```typescript
{
  id: string;
  tasks: Array<{
    id: string;
    name: string;
    durationDays: number;
    planStartDate: string;
    planFinishDate: string;
    assignedUserIds: string[];
    isOnCriticalPath: boolean;
  }>;
}
```

#### PUT /api/v1/projects/{id}/tasks/{taskId}
- **路径**: `/api/v1/projects/{id}/tasks/{taskId}`
- **请求**:
```typescript
{
  planStartDate?: string;
  planFinishDate?: string;
  durationDays?: number;
  actualStartDate?: string;
  actualFinishDate?: string;
  status?: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
}
```
- **响应**:
```typescript
{
  id: string;
  updatedAt: string;
  criticalPathUpdated: boolean;
}
```

#### POST /api/v1/projects/{id}/tasks/{taskId}/dependencies
- **路径**: `/api/v1/projects/{id}/tasks/{taskId}/dependencies`
- **请求**:
```typescript
{
  fromTaskId: string;
  type: 'FS' | 'SS' | 'FF' | 'SF';
  lagDays?: number;
}
```
- **响应**:
```typescript
{
  id: string;
  criticalPathUpdated: boolean;
}
```

#### POST /api/v1/projects/{id}/publish
- **路径**: `/api/v1/projects/{id}/publish`
- **请求**: 无
- **响应**:
```typescript
{
  publishedAt: string;
  notificationCount: number;
}
```

### 3.3 个人任务API

#### GET /api/v1/me/tasks
- **路径**: `/api/v1/me/tasks?status=&projectId=`
- **请求**: Query参数
- **响应**:
```typescript
Array<{
  id: string;
  name: string;
  projectId: string;
  projectName: string;
  planStartDate: string;
  planFinishDate: string;
  actualStartDate?: string;
  actualFinishDate?: string;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  isKcp: boolean;
}>
```

#### POST /api/v1/me/tasks/{taskId}/start
- **路径**: `/api/v1/me/tasks/{taskId}/start`
- **请求**: 无
- **响应**:
```typescript
{
  id: string;
  actualStartDate: string;
  status: 'IN_PROGRESS';
}
```

#### POST /api/v1/me/tasks/{taskId}/complete
- **路径**: `/api/v1/me/tasks/{taskId}/complete`
- **请求**: 无
- **响应**:
```typescript
{
  id: string;
  actualFinishDate: string;
  status: 'COMPLETED';
}
```

### 3.4 基础数据API

#### POST /api/v1/admin/sync-users
- **路径**: `/api/v1/admin/sync-users`
- **请求**: 无
- **响应**:
```typescript
{
  syncedCount: number;
  syncedAt: string;
}
```

#### GET /api/v1/positions
- **路径**: `/api/v1/positions?activeOnly=`
- **请求**: Query参数
- **响应**:
```typescript
Array<{
  id: string;
  code: string;
  name: string;
  isActive: boolean;
  referenceCount: number; // 被模板引用的次数
}>
```

#### DELETE /api/v1/positions/{id}
- **路径**: `/api/v1/positions/{id}`
- **请求**: 无
- **响应**:
```typescript
{
  deleted: boolean;
}
```

## 4. 异常处理
- **400**: 请求参数校验失败
- **403**: 权限不足
- **404**: 资源不存在
- **409**: 业务规则冲突（循环依赖、岗位被引用等）
- **500**: 服务器内部错误

## 5. 测试用例

### 5.1 模板循环依赖检测
- **输入**:
```typescript
const tasks = [
  {id: 'A', dependencies: [{taskId: 'B', type: 'FS'}]},
  {id: 'B', dependencies: [{taskId: 'C', type: 'FS'}]},
  {id: 'C', dependencies: [{taskId: 'A', type: 'FS'}]}
];
```
- **输出**: 抛出错误"检测到循环依赖"

### 5.2 关键路径计算
- **输入**:
```typescript
const tasks = [
  {id: 'A', duration: 5, dependencies: []},
  {id: 'B', duration: 3, dependencies: [{taskId: 'A', type: 'FS'}]},
  {id: 'C', duration: 4, dependencies: [{taskId: 'A', type: 'FS'}]},
  {id: 'D', duration: 2, dependencies: [{taskId: 'B', type: 'FS'}, {taskId: 'C', type: 'FS'}]}
];
const startDate = '2024-01-01';
```
- **输出**:
```typescript
{
  'A': {isCritical: true},
  'C': {isCritical: true},
  'D': {isCritical: true},
  'B': {isCritical: false}
}
```

### 5.3 任务时间校验
- **输入**:
```typescript
const update = {
  actualStartDate: '2024-01-10',
  actualFinishDate: '2024-01-05'
};
```
- **输出**: 抛出错误"实际完成时间不能早于开始时间"

### 5.4 岗位删除约束
- **输入**: 删除已被模板引用的岗位ID
- **输出**: 抛出错误"该岗位已被模板引用，无法删除"

---

## Machine-Readable JSON（附录）

```json

```
