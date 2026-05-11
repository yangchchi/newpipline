# AI 赋能平台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: 你必须严格按顺序完成每个 Task，并确保每个 `- [ ]` 步骤的 `Run` 命令执行成功，`Expected` 结果得到验证后，才可勾选推进。所有文件路径请使用反引号包裹。

**Goal:** 构建一站式AI赋能平台，解决员工“AI焦虑”。提供AI工具搜索、基于内部知识库的智能问答、AI项目需求提交与认领、知识库文档管理等功能，降低AI使用门槛。

**Architecture:** 前后端分离。后端采用 Node.js + TypeScript 的 Web 框架，提供 RESTful API。前端为 React + TypeScript 应用。PostgreSQL 存储主业务数据，Redis 用于缓存/队列，向量数据库用于 RAG 知识检索。使用 Bull/Agenda 处理异步任务（如文档处理）。

**Tech Stack:** 后端：Node.js, TypeScript, Express/Fastify, Zod, Bull/Agenda；前端：React, TypeScript, Next.js (可选)；数据库：PostgreSQL, Redis；向量数据库：Pinecone/Chroma；其他：Jest/Testing Library (测试)。

---
## 0. 执行约定（必须先完成）

**Files:**
- `package.json`
- `README.md`
- `.gitignore`
- 数据库迁移/种子文件（按实际仓库调整）

- [ ] **Step 1: 初始化项目与探测**
    - Run: `ls -la` 或检查 `package.json` 以确定现有项目结构（是全新项目还是已有部分代码）。
    - Expected: 确认项目根目录，了解后端(`/server`)、前端(`/client`或`/web`)、数据库脚本等主要目录位置。记录下用于后续步骤。

- [ ] **Step 2: 建立开发分支**
    - Run: `git checkout -b feat/ai-empowerment-platform-mvp` 或按实际仓库分支策略调整。
    - Expected: 成功创建并切换到新功能分支。

- [ ] **Step 3: 安装核心依赖**
    - Run: 根据探测结果，在对应目录运行 `npm install` 或 `yarn install`。若无 `package.json`，则按 TS 初始化。
    - Expected: `node_modules` 安装完毕，无重大错误。

- [ ] **Step 4: 数据库初始化**
    - Run: 按实际仓库的数据库配置，运行迁移脚本（如 `npm run db:migrate`）或执行提供的 SQL 文件。
    - Expected: PostgreSQL 中创建 `users`, `tools`, `demands`, `knowledge_documents` 等表结构，与 TS 数据模型一致。

### Task 1: 实现核心数据模型与 API 框架

**Files:**
- Create: `server/src/domain/*.ts` (User, Tool, Demand, KnowledgeDocument 接口/类型)
- Create: `server/src/middleware/validation.ts` (Zod 模式与验证中间件)
- Create: `server/src/middleware/auth.ts` (身份验证与权限中间件)
- Create: `server/src/middleware/errorHandler.ts` (统一错误处理中间件)
- Modify: `server/src/app.ts` 或 `server/src/index.ts` (集成中间件与路由)
- Test: `server/src/__tests__/middleware/*.test.ts`

- [ ] **Step 1.1: 定义领域模型**
    - Run: 创建 `server/src/domain/` 目录及文件，根据 TS 数据模型设计部分，用 TypeScript 定义 `User`, `Tool`, `Demand`, `KnowledgeDocument` 接口和枚举。
    - Expected: 接口定义完整，类型安全，可直接用于后续业务逻辑和数据库ORM。

- [ ] **Step 1.2: 实现 Zod 验证与统一错误响应**
    - Run: 在 `validation.ts` 中，为 `CreateDemandRequest` 等 API 请求体定义 Zod Schema。在 `errorHandler.ts` 中实现中间件，确保所有 API 错误均按 TS 异常处理规则返回 `{ code, message, details? }` 格式。
    - Expected: 能通过单元测试验证 Zod 模式对有效/无效输入的校验，错误处理器能捕获并格式化各种 HTTP 错误。

- [ ] **Step 1.3: 实现认证与授权中间件**
    - Run: 在 `auth.ts` 中实现 `authenticate` (检查登录状态) 和 `authorize` (检查用户角色) 中间件。结合 FS 身份验证规则与权限控制规则。
    - Expected: 中间件能正确拦截未登录请求(返回401)，并对无权限访问后台的请求返回403。可通过模拟请求测试。

- [ ] **Step 1.4: 集成中间件与基础路由**
    - Run: 在 `app.ts` 中按顺序集成错误处理、认证等中间件。创建 `/api/v1/health` 等测试路由。
    - Expected: 应用启动成功，访问 `/api/v1/health` 返回 200 OK，错误处理中间件工作正常。

### Task 2: 实现工具搜索功能 (用户故事1 & 搜索规则)

**Files:**
- Create: `server/src/routes/tool.routes.ts`
- Create: `server/src/controllers/tool.controller.ts`
- Create: `server/src/services/tool.service.ts`
- Modify: `server/src/app.ts` (注册工具路由)
- Create: `server/src/__tests__/api/tool.search.test.ts`
- Create/Modify: `client/.../ToolSearchPage.tsx` 或 `client/.../components/ToolSearchBar.tsx` (前端组件)
- Test: `client/.../__tests__/ToolSearch.test.tsx`

- [ ] **Step 2.1: 实现工具搜索 API (GET /api/v1/tools/search)**
    - Run: 在 `service` 层实现根据 `keyword` 在 `tools` 表中搜索 `name`, `description`, `scenarios` 字段的逻辑，并过滤 `isActive=true`。控制器处理请求/响应，错误处理。
    - Expected: API 符合 TS 设计。通过测试用例（如搜索“图像生成”）返回正确结果，无结果时返回空数组。

- [ ] **Step 2.2: 实现全局与局部搜索规则**
    - Run: 根据 FS 搜索范围规则，可能需要创建另一个聚合搜索端点 `/api/v1/search/global`，或在现有工具搜索逻辑中通过查询参数区分范围。确保工具广场搜索仅限 `tools` 表。
    - Expected: 全局搜索能跨工具、案例、教程检索（需相关模型），工具广场搜索仅限工具。可通过不同路由或参数测试验证。

- [ ] **Step 2.3: 实现前端工具搜索界面**
    - Run: 在工具广场页面或全局导航栏实现搜索输入框。输入关键词后，调用搜索 API，将结果以卡片列表形式渲染，显示名称、简介、标签。无结果时显示指定提示语。
    - Expected: 前端界面与用户故事1验收标准一致。输入关键词触发搜索并展示结果，无结果时显示友好提示。

### Task 3: 实现需求提交流程 (用户故事3 & 全局规则)

**Files:**
- Create: `server/src/routes/demand.routes.ts`
- Create: `server/src/controllers/demand.controller.ts`
- Create: `server/src/services/demand.service.ts`
- Modify: `server/src/app.ts` (注册需求路由)
- Create: `server/src/__tests__/api/demand.create.test.ts`
- Create/Modify: `client/.../DemandSubmissionPage.tsx` (需求提交表单)
- Test: `client/.../__tests__/DemandSubmission.test.tsx`

- [ ] **Step 3.1: 实现需求提交 API (POST /api/v1/demands)**
    - Run: 在 `service` 层实现创建需求逻辑，状态初始化为 `pending_review`。控制器使用 Zod 验证请求体，并集成 `authenticate` 中间件确保用户登录。
    - Expected: API 符合 TS 设计。成功创建返回201，验证失败返回400及具体字段错误。通过 TS 提供的验证失败测试用例。

- [ ] **Step 3.2: 实现需求提交表单前端**
    - Run: 实现包含标题、描述、业务价值、期望完成时间、联系人邮箱的表单。“提交”按钮初始禁用，前端实时校验必填字段，全部有效后启用。提交时调用 API。
    - Expected: 表单交互与用户故事3验收标准一致。必填字段为空时按钮禁用，提交时前端校验并提示。

- [ ] **Step 3.3: 集成后台通知（模拟）**
    - Run: 在需求创建成功后，模拟“通知内容运营人员”动作（如打印日志、向特定队列发送消息）。
    - Expected: 创建需求后，能在日志或队列中观察到通知事件。

### Task 4: 实现需求认领流程 (用户故事4 & 规则)

**Files:**
- Modify: `server/src/services/demand.service.ts` (添加 `claimDemand` 方法)
- Modify: `server/src/controllers/demand.controller.ts` (添加 `claim` 端点处理)
- Modify: `server/src/routes/demand.routes.ts` (添加 `POST /:demandId/claim` 路由)
- Create: `server/src/__tests__/api/demand.claim.test.ts`
- Create/Modify: `client/.../DemandDetailPage.tsx` (需求详情与认领按钮)
- Test: `client/.../__tests__/DemandClaim.test.tsx`

- [ ] **Step 4.1: 实现需求认领 API (POST /api/v1/demands/:demandId/claim)**
    - Run: 在 `service` 层实现认领逻辑：检查需求存在且状态为 `open`（待认领），检查当前用户角色为 `ai_expert`，更新状态为 `in_progress`，记录认领者与时间。集成 `authenticate` 和 `authorize` 中间件。
    - Expected: API 符合 TS 设计。成功认领返回200，状态冲突返回409，非专家角色返回403。通过 TS 提供的冲突测试用例。

- [ ] **Step 4.2: 实现需求认领前端逻辑**
    - Run: 在需求详情页，根据需求状态动态显示“认领此需求”按钮（仅当状态为 `open` 且用户角色为专家）。点击后调用认领 API，成功后更新页面状态并显示认领者信息。
    - Expected: 前端交互与用户故事4验收标准一致。按钮显示条件正确，认领成功后状态和信息更新。

- [ ] **Step 4.3: 实现站内消息通知（模拟）**
    - Run: 在认领成功后，模拟“通过站内消息通知原提交者”动作。
    - Expected: 认领成功后，能观察到通知事件（包含需求标题和专家姓名）。

### Task 5: 实现智能问答 RAG 功能 (用户故事2)

**Files:**
- Create: `server/src/services/rag.service.ts` (RAG 核心逻辑)
- Create: `server/src/routes/qa.routes.ts`
- Create: `server/src/controllers/qa.controller.ts`
- Modify: `server/src/app.ts` (注册问答路由)
- Create: `server/src/__tests__/api/qa.ask.test.ts`
- Create/Modify: `client/.../QAPage.tsx` (问答聊天界面)
- Test: `client/.../__tests__/QAPage.test.tsx`

- [ ] **Step 5.1: 搭建 RAG 服务框架**
    - Run: 在 `rag.service.ts` 中实现框架：接收问题 -> 向量化 -> 在向量数据库（如 Pinecone）中检索相关文档片段 -> 调用 LLM (如 OpenAI API) 生成答案 -> 格式化答案与引用。
    - Expected: 服务能处理问答请求，返回包含答案和参考来源的对象。先使用模拟数据或简单检索验证流程。

- [ ] **Step 5.2: 实现问答 API (POST /api/v1/qa/ask)**
    - Run: 控制器集成 RAG 服务，处理请求/响应。实现频率限制中间件（返回429）。处理知识库无结果情况，返回指定答案。
    - Expected: API 符合 TS 设计。能返回带引用的答案或无结果的标准回复。通过 TS 提供的无结果测试用例。

- [ ] **Step 5.3: 实现前端问答界面**
    - Run: 实现聊天式界面：输入框、发送按钮、对话历史区域。提问时显示加载动画。答案以气泡形式展示，末尾注明参考来源。
    - Expected: 前端交互与用户故事2验收标准一致。加载动画、答案展示格式、无结果提示均符合要求。

### Task 6: 实现知识库文档管理 (用户故事5 & 上传规则)

**Files:**
- Create: `server/src/routes/knowledge.routes.ts`
- Create: `server/src/controllers/knowledge.controller.ts`
- Create: `server/src/services/knowledge.service.ts` (包含文件校验、上传、异步处理队列)
- Modify: `server/src/app.ts` (注册知识库路由)
- Create: `server/src/__tests__/api/knowledge.upload.test.ts`
- Create/Modify: `client/.../admin/KnowledgeManagementPage.tsx` (后台管理界面)
- Test: `client/.../__tests__/admin/KnowledgeUpload.test.tsx`

- [ ] **Step 6.1: 实现文档上传 API (POST /api/v1/knowledge/documents)**
    - Run: 使用 `multer` 等中间件处理 `multipart/form-data`。在控制器中进行文件格式(.pdf, .docx, .txt)和大小(≤50MB)校验。校验通过后将文件信息存入数据库（状态`processing`），并提交异步处理任务到 Bull/Agenda 队列。返回202。
    - Expected: API 符合 TS 设计。成功返回202，文件过大返回413，格式错误返回400。通过 TS 提供的文件过大测试用例。

- [ ] **Step 6.2: 实现前端文档上传界面**
    - Run: 在后台管理页面实现“上传文档”按钮、文件选择器。选择文件后立即进行前端格式和大小校验，显示对应提示。通过后调用上传 API。
    - Expected: 前端交互与用户故事5验收标准一致。即时校验提示、上传后列表显示“处理中”记录。

- [ ] **Step 6.3: 实现文档异步处理 Worker**
    - Run: 创建独立的 Worker 进程或脚本，监听文档处理队列。任务：解析文档文本 -> 分块 -> 向量化 -> 存入向量数据库 -> 更新文档状态为 `available`。处理失败则更新状态为 `failed` 并记录错误。
    - Expected: 上传文件后，Worker 能正确消费任务，更新数据库中文档状态。

### Task 7: 实现后台管理与业务规则 (FS 规则补充)

**Files:**
- Modify: `server/src/services/demand.service.ts` (添加需求超时检查逻辑)
- Create: `server/src/scripts/check-demand-timeout.ts` (定时任务脚本)
- Modify: `server/src/middleware/auth.ts` (强化后台路由权限控制)
- Create/Modify: `client/.../admin/*.tsx` (后台管理相关页面)
- Modify: 各服务层逻辑，集成内容可见性规则 (`isActive`, `status` 过滤)

- [ ] **Step 7.1: 实现需求超时规则与定时任务**
    - Run: 在 `demand.service` 中添加方法，查询创建超过7天且状态为 `open` 的需求。编写定时任务脚本（可通过 Agenda 调度），定期执行该方法并模拟“发送邮件通知运营管理员”。
    - Expected: 定时任务能正确筛选出超时需求，并触发通知事件。

- [ ] **Step 7.2: 强化权限控制**
    - Run: 确保所有以 `/admin` 或 `/api/v1/knowledge`（除公开读操作）开头的后端路由，都受到 `authorize` 中间件保护，仅允许 `content_operator` 或 `admin` 角色访问。
    - Expected: 非运营/管理员角色用户尝试访问后台 API 时，返回 403 错误。

- [ ] **Step 7.3: 实现内容可见性规则**
    - Run: 在所有查询工具、案例、教程的 `service` 方法中（如 `tool.service.search`），加入 `isActive = true` 或等效的状态过滤条件。对于直接通过ID访问详情的接口，若内容已下线，返回特定提示或404。
    - Expected: 被标记为下线的内容不会出现在列表和搜索结果中，直接访问详情页有相应提示。

### Task 8: 端到端测试、部署准备与文档

**Files:**
- Create: `cypress/e2e/` 或类似目录的端到端测试文件
- Modify: `package.json` (添加 e2e 脚本)
- Create: `docker-compose.yml` (用于本地开发)
- Create: `DEPLOYMENT.md` 或更新 `README.md`
- Create: `server/src/__tests__/integration/*.test.ts`

- [ ] **Step 8.1: 编写关键用户流程的端到端测试**
    - Run: 使用 Cypress 或类似工具，编写测试用例：1) 搜索工具并查看结果；2) 登录、提交需求、在后台审核；3) 专家认领需求；4) 上传知识文档并触发问答。
    - Expected: 端到端测试能自动化执行核心用户故事流程并通过。

- [ ] **Step 8.2: 创建 Docker 开发环境**
    - Run: 编写 `docker-compose.yml`，包含 PostgreSQL, Redis, 以及可选向量数据库的服务定义。确保应用可以通过 `docker-compose up` 一键启动所有依赖。
    - Expected: 运行 `docker-compose up` 后，数据库和缓存服务就绪，应用可以连接。

- [ ] **Step 8.3: 编写部署与配置文档**
    - Run: 创建 `DEPLOYMENT.md`，说明环境变量（数据库连接字符串、向量数据库API密钥、JWT密钥等）、构建步骤、数据库迁移命令、生产环境注意事项。
    - Expected: 文档清晰，他人可根据文档部署应用到生产或测试环境。

## 全局验收标准（完成本计划前必须全部满足）

- [ ] **业务功能覆盖**：用户故事1-5的所有验收标准均实现并通过测试。
- [ ] **全局规则满足**：身份验证、权限控制、需求超时、搜索范围、内容可见性等FS补充规则全部生效。
- [ ] **API 规范符合**：所有后端 API 的路径、请求/响应格式、错误码均严格遵循 TS 设计。
- [ ] **数据模型一致**：数据库表结构与 TS 数据模型设计保持一致，包含所有必要字段和约束。
- [ ] **错误处理统一**：所有 API 错误均以 `{ code, message, details? }` 格式返回，且 HTTP 状态码符合 TS 异常处理规则。
- [ ] **测试覆盖充分**：核心业务逻辑（服务层）、API 端点、关键前端组件均有单元测试；核心用户流程有端到端测试。
- [ ] **身份与权限**：登录、角色校验、后台保护功能工作正常，无越权访问漏洞。
- [ ] **异步处理可靠**：知识库文档上传后的异步处理流程（队列、Worker）能稳定运行，成功与失败状态可追踪。
- [ ] **前端体验达标**：搜索、表单、问答等界面交互流畅，符合验收标准中的加载、提示、禁用等状态要求。
- [ ] **代码质量**：TypeScript 类型定义完整，无重大 `any` 类型；关键函数有注释；代码经过格式化，无低级错误。