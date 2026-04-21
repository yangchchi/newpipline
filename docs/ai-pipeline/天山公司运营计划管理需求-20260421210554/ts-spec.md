# 技术规格（TS）：spec-1776776032371

## 元信息

- 流水线：天山公司运营计划管理需求-生产流水线
- 规格 ID：spec-1776776032371
- 关联 PRD：prd-1776776000345
- 状态：draft
- 更新时间：2026-04-21T13:05:39.535Z


---

# TS 正文（Markdown）

# TS

## 1. 技术栈
- 后端：Node.js 20 LTS + TypeScript 5+
- 前端：React + Gantt 组件库 (如 dhtmlx-gantt/frappe-gantt)
- 数据库：PostgreSQL 15+
- 中间件：Redis (缓存人员数据)

## 2. 数据模型设计

### domain/Template.ts
```typescript
export interface Template {
  id: string;
  name: string;
  description: string;
  tasks: TemplateTask[];
}

export interface TemplateTask {
  id: string;
  name: string;
  duration: number; // 工作日
  position: string; // 关联岗位，如 "机械工程师"
  dependencies: string[]; // 前置任务ID列表
}
```

### domain/ProjectPlan.ts
```typescript
export interface ProjectPlan {
  id: string;
  name: string;
  startDate: Date;
  status: 'draft' | 'active' | 'completed';
  tasks: PlanTask[];
  criticalPath: string[]; // 关键路径任务ID列表
  createdAt: Date;
  updatedAt: Date;
}

export interface PlanTask {
  id: string;
  name: string;
  duration: number;
  position: string | null;
  assigneeId: string | null;
  assigneeName: string | null;
  dependencies: string[];
  es: Date; // 最早开始时间
  ef: Date; // 最早结束时间
  ls: Date; // 最晚开始时间
  lf: Date; // 最晚结束时间
  isCritical: boolean;
}
```

### domain/Personnel.ts
```typescript
export interface Personnel {
  id: string;
  name: string;
  position: string;
  department: string;
}

export interface PersonnelAssignment {
  personnelId: string;
  taskId: string;
  planId: string;
  taskStart: Date;
  taskEnd: Date;
}
```

## 3. API设计

### 3.1 获取模板列表
- 路径：`GET /api/v1/templates`
- 请求：无
- 响应：
```typescript
{
  data: Array<{
    id: string;
    name: string;
    description: string;
  }>;
}
```
- 错误码：200, 500

### 3.2 基于模板创建计划
- 路径：`POST /api/v1/plans`
- 请求：
```typescript
{
  templateId: string;
  projectName: string;
  startDate: string; // ISO 8601
}
```
- 响应：
```typescript
{
  data: {
    planId: string;
    tasks: Array<{
      id: string;
      name: string;
      duration: number;
      position: string | null;
      es: string; // ISO 8601
      ef: string;
      ls: string;
      lf: string;
      isCritical: boolean;
      dependencies: string[];
    }>;
    criticalPath: string[];
  };
}
```
- 错误码：
  - 400: 项目名称为空
  - 404: 模板不存在
  - 500: 计算失败

### 3.3 调整任务工期
- 路径：`PATCH /api/v1/plans/:planId/tasks/:taskId/duration`
- 请求：
```typescript
{
  newDuration: number; // > 0
}
```
- 响应：
```typescript
{
  data: {
    updatedTasks: Array<{
      id: string;
      es: string;
      ef: string;
      ls: string;
      lf: string;
      isCritical: boolean;
    }>;
    criticalPath: string[];
    totalDuration: number; // 项目总工期
  };
}
```
- 错误码：
  - 400: 新工期≤0或循环依赖
  - 404: 计划或任务不存在
  - 500: 计算失败

### 3.4 获取可分配人员列表
- 路径：`GET /api/v1/personnel`
- 查询参数：`position?=机械工程师`
- 响应：
```typescript
{
  data: Array<{
    id: string;
    name: string;
    position: string;
    hasConflict?: boolean; // 前端根据任务时间计算
    conflictTasks?: string[]; // 冲突的任务名称
  }>;
  meta: {
    syncStatus: 'success' | 'pending' | 'failed';
    lastSyncedAt: string | null;
  };
}
```
- 错误码：200, 500

### 3.5 分配任务负责人
- 路径：`POST /api/v1/plans/:planId/tasks/:taskId/assignee`
- 请求：
```typescript
{
  personnelId: string;
  forceAssign?: boolean; // 强制分配（即使有时间冲突）
}
```
- 响应：
```typescript
{
  data: {
    taskId: string;
    assigneeId: string;
    assigneeName: string;
    hasConflict: boolean;
    conflictLogId?: string; // 如果强制分配，记录冲突日志ID
  };
}
```
- 错误码：
  - 400: 人员不存在或未关联岗位
  - 404: 计划或任务不存在
  - 409: 时间冲突且未强制分配
  - 500: 保存失败

## 4. 异常处理
- 所有4xx错误响应结构：
```typescript
{
  error: {
    code: string; // 如 "INVALID_INPUT"
    message: string;
    details?: Record<string, unknown>;
  };
}
```
- 业务规则错误码：
  - `PROJECT_NAME_REQUIRED`: 项目名称为空
  - `DURATION_INVALID`: 工期≤0
  - `CIRCULAR_DEPENDENCY`: 循环依赖
  - `PERSONNEL_CONFLICT`: 人员时间冲突
  - `TEMPLATE_NOT_FOUND`: 模板不存在
- 5xx错误记录请求ID并告警

## 5. 测试用例

### 5.1 CPM计算函数测试
- 输入：
```typescript
const tasks = [
  { id: 'A', duration: 3, dependencies: [] },
  { id: 'B', duration: 2, dependencies: ['A'] },
  { id: 'C', duration: 4, dependencies: ['A'] },
  { id: 'D', duration: 3, dependencies: ['B', 'C'] }
];
const startDate = new Date('2024-06-01');
```
- 输出：
```typescript
{
  'A': { es: '2024-06-01', ef: '2024-06-03', ls: '2024-06-01', lf: '2024-06-03', isCritical: true },
  'B': { es: '2024-06-04', ef: '2024-06-05', ls: '2024-06-06', lf: '2024-06-07', isCritical: false },
  'C': { es: '2024-06-04', ef: '2024-06-07', ls: '2024-06-04', lf: '2024-06-07', isCritical: true },
  'D': { es: '2024-06-08', ef: '2024-06-10', ls: '2024-06-08', lf: '2024-06-10', isCritical: true },
  criticalPath: ['A', 'C', 'D']
}
```

### 5.2 创建计划API测试
- 输入：
```typescript
POST /api/v1/plans
{
  "templateId": "std-product-delivery",
  "projectName": "A型机订单2024-001",
  "startDate": "2024-06-01"
}
```
- 输出：
```typescript
201 Created
{
  "data": {
    "planId": "plan_123",
    "tasks": [...], // 50个任务
    "criticalPath": ["task_1", "task_3", "task_7"...]
  }
}
```

### 5.3 人员冲突检测测试
- 输入：
```typescript
任务时间：2024-06-01 至 2024-06-05
人员现有分配：[
  { taskId: 'task_x', taskStart: '2024-06-03', taskEnd: '2024-06-07' }
]
```
- 输出：
```typescript
{
  hasConflict: true,
  conflictTasks: ['B项目外壳设计'],
  overlapDays: 2
}
```

### 5.4 异常流程测试
- 输入：
```typescript
POST /api/v1/plans
{
  "templateId": "std-product-delivery",
  "projectName": "",
  "startDate": "2024-06-01"
}
```
- 输出：
```typescript
400 Bad Request
{
  "error": {
    "code": "PROJECT_NAME_REQUIRED",
    "message": "项目名称不能为空"
  }
}
```

---

## Machine-Readable JSON（附录）

```json

```
