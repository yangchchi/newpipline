# CP

> **For agentic workers:** REQUIRED SUB-SKILL: 理解前后端分离架构、TypeScript、PostgreSQL 数据模型、TDD 开发流程，能够按任务顺序执行实现与验证，每个步骤必须检查 `Expected:` 结果。

**Goal:** 构建一个可视化的运营计划管理系统（PMS），以缩短计划编制时间、确保计划调整后关键路径重算准确、提升 KCP 按时达成率，并沉淀可复用的计划模板。

**Architecture:** 基于 Node.js + TypeScript 的后端提供 RESTful API，Next.js + Tailwind + TypeScript + ESLint + Materio Next.js Admin Template 前端提供交互界面，PostgreSQL 作为主数据存储，Redis 用于缓存 （开发环境暂不实现）。

**Tech Stack:** 后端：Node.js, TypeScript, PostgreSQL；前端：React, TypeScript；数据库：PostgreSQL；缓存：Redis。

---

## 0. 执行约定（必须先完成）

**Files:** `package.json`, `README.md`, `.gitignore`, `docker-compose.yml` (用于启动 PostgreSQL 和 Redis)

- [ ] **Step 1: 初始化项目结构与依赖**
    Run: `mkdir -p backend frontend && cd backend && npm init -y && cd ../frontend && npm create vite@latest . -- --template react-ts`
    Expected: 创建 `backend/` 和 `frontend/` 目录，并分别初始化 Node.js 项目和 Vite React 项目。

- [ ] **Step 2: 配置数据库与缓存服务**
    Run: 在项目根目录创建 `docker-compose.yml` 文件，定义 PostgreSQL 15 和 Redis 7 服务。
    Expected: 运行 `docker-compose up -d` 后，可通过 `docker ps` 看到两个容器正在运行。

- [ ] **Step 3: 建立代码仓库与分支策略**
    Run: `git init && git add . && git commit -m “Initial commit”`。约定 `main` 为保护分支，新功能在 `feat/*` 分支开发。
    Expected: 本地 Git 仓库初始化完成，拥有首次提交记录。

---

## Task 1: 搭建基础框架与数据模型

**Files:** Create: `backend/tsconfig.json`, `backend/src/index.ts`, `backend/src/db/migrations/001_init.sql`, `backend/src/db/schema.sql`; Modify: `backend/package.json`

- [ ] **Step 1.1: 配置后端 TypeScript 与基础依赖**
    Run: `cd backend && npm install typescript ts-node @types/node express pg redis dotenv && npm install --save-dev @types/express nodemon`
    Expected: `backend/package.json` 的 `dependencies` 和 `devDependencies` 包含上述包。

- [ ] **Step 2.1: 实现数据库迁移脚本**
    Run: 创建 `backend/src/db/migrations/001_init.sql`，内容为 TS 中 `数据模型设计` 部分的全部 `CREATE TABLE` 语句。
    Expected: 运行 `psql -U postgres -d pms -f backend/src/db/migrations/001_init.sql` (按实际连接信息调整) 后，所有表被成功创建。

- [ ] **Step 3.1: 创建数据库连接与模型定义文件**
    Run: 创建 `backend/src/db/schema.sql` (复制迁移脚本内容) 和 `backend/src/db/index.ts`，后者导出配置好的 `pg.Pool` 和 `Redis` 客户端实例。
    Expected: 在 `backend/src/index.ts` 中导入并测试数据库连接，控制台输出连接成功信息。

---

## Task 2: 实现模板管理核心功能（后端）

**Files:** Create: `backend/src/services/templateService.ts`, `backend/src/controllers/templateController.ts`, `backend/src/routes/templateRoutes.ts`, `backend/src/validators/templateValidator.ts`; Test: `backend/tests/templateService.test.ts`

- [ ] **Step 2.1: 实现模板循环依赖检测算法 (TDD)**
    Run: 在 `templateService.test.ts` 中编写测试，使用 TS 中 `测试用例 5.1` 的数据调用待实现的 `detectCycle` 函数。
    Expected: 测试失败（红）。
    Run: 在 `templateService.ts` 中实现 `detectCycle` 函数（使用拓扑排序或 DFS）。
    Expected: 测试通过（绿）。

- [ ] **Step 2.2: 实现模板创建与版本化逻辑**
    Run: 在 `templateService.ts` 中实现 `createTemplate` 函数，逻辑包括：
        1. 校验输入（名称、任务非空，工期为正）。
        2. 调用 `detectCycle`。
        3. 在 `templates` 表插入记录，在 `template_versions` 表插入版本数据（`data` 字段存整个模板 JSON），在 `template_tasks` 表扁平化存储节点。
    Expected: 针对 `POST /api/v1/templates` 的控制器测试能成功返回 `id` 和 `version`。

- [ ] **Step 2.3: 实现模板列表、详情、更新与版本历史 API**
    Run: 在 `templateController.ts` 和 `templateRoutes.ts` 中实现：
        `GET /api/v1/templates` (带分页筛选)
        `PUT /api/v1/templates/{id}` (更新并生成新版本)
        `GET /api/v1/templates/{id}/versions`
        `POST /api/v1/templates/{id}/rollback/{version}`
    Expected: 使用 `curl` 或 Postman 测试各端点，能按规格正确响应。

---

## Task 3: 实现项目管理与关键路径计算（后端）

**Files:** Create: `backend/src/services/projectService.ts`, `backend/src/services/criticalPathService.ts`, `backend/src/controllers/projectController.ts`; Modify: `backend/src/routes/projectRoutes.ts`

- [ ] **Step 3.1: 实现基于模板生成项目计划 (TDD)**
    Run: 在 `projectService.test.ts` 中编写测试，模拟 `POST /api/v1/projects` 请求，验证生成的 `project_tasks` 中 `plan_start_date` 和 `plan_finish_date` 计算正确。
    Expected: 测试失败（红）。
    Run: 在 `projectService.ts` 中实现 `createProjectFromTemplate` 函数，根据模板结构、依赖关系、`planStartDate` 进行正向计算。
    Expected: 测试通过（绿），任务日期计算正确。

- [ ] **Step 3.2: 实现关键路径计算与更新服务**
    Run: 在 `criticalPathService.ts` 中实现 `calculateCriticalPath` 函数，算法（CPM）输入为项目任务列表，输出每个任务的 `isOnCriticalPath` 布尔值。参考 TS 中 `测试用例 5.2`。
    Expected: 对给定测试用例，函数返回预期结果。
    Run: 在 `projectService.ts` 中实现 `updateTask` 函数，当任务计划时间、工期或依赖变更时（规则 R2），调用 `calculateCriticalPath` 并更新相关任务的 `is_on_critical_path` 字段。
    Expected: 更新任务后，数据库中的关键路径标记被正确更新。

- [ ] **Step 3.3: 实现项目任务更新、依赖管理、发布 API**
    Run: 实现 `PUT /api/v1/projects/{id}/tasks/{taskId}` (包含规则 R3 校验)、`POST /api/v1/projects/{id}/tasks/{taskId}/dependencies`、`POST /api/v1/projects/{id}/publish` (模拟发送通知)。
    Expected: API 能处理请求，并正确触发关键路径重算（如需要）。

---

## Task 4: 实现用户、岗位与个人任务 API（后端）

**Files:** Create: `backend/src/services/userService.ts`, `backend/src/services/positionService.ts`, `backend/src/controllers/meController.ts`; Modify: `backend/src/routes/adminRoutes.ts`, `backend/src/routes/positionRoutes.ts`

- [ ] **Step 4.1: 实现用户同步与岗位管理 API**
    Run: 实现 `POST /api/v1/admin/sync-users`（模拟调用外部接口，实际更新 `users` 表）。
    Run: 实现 `GET /api/v1/positions` 和 `DELETE /api/v1/positions/{id}`。在删除前检查 `referenceCount`（需写 SQL 查询 `template_tasks` 表），执行规则 R4。
    Expected: 删除被引用的岗位返回 409 错误。

- [ ] **Step 4.2: 实现个人任务获取与状态更新 API**
    Run: 实现 `GET /api/v1/me/tasks`，根据当前登录用户 ID（可从模拟的请求上下文中获取）过滤 `project_tasks` 表。
    Run: 实现 `POST /api/v1/me/tasks/{taskId}/start` 和 `.../complete`，自动设置时间戳和状态。
    Expected: API 返回更新后的任务信息，且 `actual_start_date` 或 `actual_finish_date` 被正确填充。

---

## Task 5: 构建前端模板管理页面

**Files:** Create: `frontend/src/pages/TemplateManagement.tsx`, `frontend/src/components/TemplateEditor.tsx`, `frontend/src/api/templateApi.ts`; Modify: `frontend/src/App.tsx`, `frontend/src/main.tsx`

- [ ] **Step 5.1: 搭建模板列表与筛选组件**
    Run: 在 `TemplateManagement.tsx` 中实现搜索筛选区（类型、部门下拉框）和模板列表表格（使用 Ant Design 或类似组件）。通过 `templateApi.ts` 调用 `GET /api/v1/templates`。
    Expected: 页面加载后能显示模板列表，筛选操作能触发新的 API 调用并更新列表。

- [ ] **Step 5.2: 实现模板编辑器画布与节点交互**
    Run: 创建 `TemplateEditor.tsx` 组件，使用 `react-flow` 或 `antv x6` 库实现右侧可视化画布。支持拖拽添加节点、连线。
    Expected: 可以在画布上添加任务节点，并通过拖拽连线建立依赖关系。

- [ ] **Step 5.3: 实现模板属性面板与保存逻辑**
    Run: 在编辑器左侧实现属性面板，可编辑选中节点的名称、工期、KCP、关联岗位（多选）。实现保存按钮，调用 `POST /api/v1/templates` 或 `PUT` API，提交前进行前端校验（规则 R1 提示需依赖后端 409 错误）。
    Expected: 能成功创建或更新模板，并收到新版本号。

---

## Task 6: 构建前端项目甘特图页面

**Files:** Create: `frontend/src/pages/ProjectGantt.tsx`, `frontend/src/components/GanttChart.tsx`, `frontend/src/api/projectApi.ts`; Modify: `frontend/src/App.tsx`

- [ ] **Step 6.1: 集成甘特图可视化组件**
    Run: 在 `GanttChart.tsx` 中集成 `dhtmlx-gantt` 或 `frappe-gantt` 库。配置显示计划条形（蓝色）、实际条形（绿色）、关键路径（红色边框）、KCP（菱形）。
    Expected: 给定静态项目数据，甘特图能正确渲染任务条和依赖线。

- [ ] **Step 6.2: 实现甘特图与任务列表的双向联动**
    Run: 在 `ProjectGantt.tsx` 中布局左侧任务树状表格和右侧甘特图画布。实现数据同步：任务列表数据变化时更新甘特图，甘特图拖拽事件（如 onTaskUpdated）触发 `PUT /api/v1/projects/{id}/tasks/{taskId}` 更新后端。
    Expected: 拖拽任务条形调整时间后，前端状态更新并触发 API 调用，关键路径重新计算后甘特图视觉更新（红框）。

- [ ] **Step 6.3: 实现计划发布与视图缩放**
    Run: 实现顶部操作栏的“发布计划”按钮（调用对应 API）和视图缩放控件（天/周/月），控制甘特图的时间刻度。
    Expected: 点击发布后收到成功反馈；切换缩放级别，甘特图时间轴正确缩放。

---

## Task 7: 构建前端我的任务页与系统集成

**Files:** Create: `frontend/src/pages/MyTasks.tsx`, `frontend/src/api/meApi.ts`; Modify: `frontend/src/App.tsx` (添加路由)

- [ ] **Step 7.1: 实现个人任务列表与看板视图**
    Run: 在 `MyTasks.tsx` 中实现列表/看板，通过 `meApi.ts` 调用 `GET /api/v1/me/tasks` 获取数据。按状态（未开始、进行中、已完成）分组显示任务卡片。
    Expected: 页面显示当前用户的所有任务，信息完整。

- [ ] **Step 7.2: 实现任务进度更新操作**
    Run: 在每个任务卡片上，根据 `status` 显示“开始”或“完成”按钮。点击后分别调用 `POST /api/v1/me/tasks/{taskId}/start` 或 `/complete` API，并乐观更新本地 UI 状态。
    Expected: 点击按钮后，任务状态、按钮文本和实际时间显示立即更新。

- [ ] **Step 7.3: 实现前端路由与权限模拟**
    Run: 使用 `react-router-dom` 配置路由：`/templates`, `/projects/:id`, `/my-tasks`。在 `App.tsx` 中模拟用户登录状态与角色。
    Expected: 能通过导航访问各个页面，不同角色看到的界面/操作权限有所区别（如仅管理员可见同步用户按钮）。

---

## Task 8: 系统联调、测试与部署准备

**Files:** Create: `backend/.env.example`, `frontend/.env.example`, `backend/Dockerfile`, `frontend/Dockerfile`; Modify: `docker-compose.yml` (添加后端和前端服务)

- [ ] **Step 8.1: 配置环境变量与跨域**
    Run: 创建 `.env.example` 文件，包含 `DATABASE_URL`、`REDIS_URL`、`FRONTEND_URL` 等变量。在后端 Express 中配置 CORS，允许前端 origin。
    Expected: 前端能成功调用后端 API，无跨域错误。

- [ ] **Step 8.2: 编写端到端（E2E）测试场景**
    Run: 使用 Cypress 或 Playwright 编写一个 E2E 测试脚本，覆盖“创建模板 -> 基于模板创建项目 -> 在甘特图调整任务 -> 发布计划 -> 用户更新任务进度”的核心流程。
    Expected: E2E 测试能自动化执行并通过。

- [ ] **Step 8.3: 容器化与部署配置**
    Run: 编写 `Dockerfile` 分别用于构建后端和前端镜像。更新 `docker-compose.yml`，定义 `app-backend`、`app-frontend`、`postgres`、`redis` 服务。
    Expected: 运行 `docker-compose build && docker-compose up` 后，可通过浏览器访问 `http://localhost:3000` 看到前端应用，且功能正常。

---

## 全局验收标准（完成本计划前必须全部满足）

- [ ] **业务功能完整**：全部五个用户故事（模板管理、创建项目、甘特图调整、个人任务更新、基础数据维护）的核心验收标准均实现并通过手动测试。
- [ ] **数据模型与 API 合规**：数据库表结构与 TS 完全一致；所有定义的 API 端点均实现，请求/响应格式、错误码（400, 403, 404, 409, 500）符合规格。
- [ ] **核心规则实现**：循环依赖检测（R1）、关键路径自动重算（R2，3秒内）、任务时间校验（R3）、岗位删除约束（R4）均在后端服务中正确实现。
- [ ] **前端交互达标**：模板编辑器支持拖拽节点与连线；甘特图支持拖拽调整任务时间与创建依赖；所有用户操作有明确的成功/错误反馈。
- [ ] **测试覆盖充分**：后端核心服务（模板、项目、关键路径计算）有单元测试；至少有一个覆盖核心业务流程的 E2E 测试通过。
- [ ] **性能与运维就绪**：关键路径计算在任务数 100 以内时，重算时间小于 3 秒；项目可通过提供的 `docker-compose.yml` 一键启动所有依赖服务（DB、Redis、后端、前端）。