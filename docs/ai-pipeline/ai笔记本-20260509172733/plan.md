# CP
> **For agentic workers:** REQUIRED SUB-SKILL: 按顺序完成每个 Task 下的步骤，严格遵循 TDD 风格（先写测试，再实现，最后验证）。每个 `- [ ]` 步骤需在完成并验证后勾选。请根据实际仓库情况调整文件路径。

**Goal:** 构建一个 AI 辅助的智能笔记应用 MVP，实现信息的智能捕获（语音转写、图片 OCR）、组织（自动标签、大纲、笔记本）与检索（关键词及语义搜索），并支持多端实时同步与访客体验。

**Architecture:** 前后端分离架构。后端采用 NestJS 框架，提供 RESTful API 与 WebSocket 服务，使用 PostgreSQL 存储核心数据，Redis 管理会话与缓存，RabbitMQ 处理异步 AI 任务。前端采用 Next.js (App Router) 与 React，负责用户界面与状态管理。

**Tech Stack:** 后端: Node.js (TypeScript), NestJS, PostgreSQL, Redis, RabbitMQ, WebSocket。前端: React (TypeScript), Next.js (App Router)。

---

## 0. 执行约定（必须先完成）
**Files:** 按实际仓库调整。
- [ ] **Step 1: 初始化项目结构与依赖**
    - **Run:** 检查 `package.json`、`docker-compose.yml` 是否存在。若无，则创建基础项目骨架，并安装 TS、NestJS、Next.js、React、Prisma/TypeORM、Redis、RabbitMQ 等核心依赖。
    - **Expected:** 项目根目录包含 `backend/` 和 `frontend/` 文件夹，且能分别运行 `npm install` 成功。
- [ ] **Step 2: 配置开发环境与数据库**
    - **Run:** 启动 Docker Compose 服务 (`docker-compose up -d`)，确保 PostgreSQL、Redis、RabbitMQ 容器运行。在 backend 目录运行数据库迁移（如 `npx prisma migrate dev` 或相应 ORM 命令）。
    - **Expected:** 能连接到 PostgreSQL 数据库，且 `users`、`notes` 等表（依据 TS 中 SQL）已创建。Redis 和 RabbitMQ 连接正常。
- [ ] **Step 3: 建立 Git 分支策略**
    - **Run:** 创建并切换到开发分支 `feat/mvp-core`。确认 `.gitignore` 已包含 `node_modules`、`.env` 等。
    - **Expected:** 当前工作在 `feat/mvp-core` 分支，后续所有修改在此分支进行。

## 1. Task 1: 用户认证与基础 API 框架
**Files:**
- Create: `backend/src/auth/auth.module.ts`, `backend/src/auth/auth.service.ts`, `backend/src/auth/auth.controller.ts`, `backend/src/auth/dto/*.ts`, `backend/src/auth/guards/*.ts`
- Create: `backend/src/users/users.module.ts`, `backend/src/users/users.service.ts`
- Modify: `backend/src/main.ts` (全局管道、拦截器)
- Test: `backend/test/auth.e2e-spec.ts`, `backend/test/users.e2e-spec.ts`

- [ ] **Step 1.1: 实现用户注册 API (邮箱/手机)**
    - **Run:** 编写 `POST /api/auth/register` 的 E2E 测试，覆盖成功注册、验证码错误、用户已存在、参数格式错误等场景。
    - **Expected:** 测试失败（红）。
- [ ] **Step 1.2: 实现注册服务逻辑**
    - **Run:** 在 `AuthService` 中实现注册逻辑，包括密码哈希、验证码校验（可先模拟）、创建用户记录（区分普通用户与访客字段）。在控制器中集成，并配置全局验证管道。
    - **Expected:** Step 1.1 的测试通过（绿）。API 能按 TS 中 `RegisterRequest` 和 `AuthResponse` 格式正确响应。
- [ ] **Step 1.3: 实现登录与 JWT 签发**
    - **Run:** 编写 `POST /api/auth/login` 测试并实现。使用 `@nestjs/jwt` 签发 accessToken 和 refreshToken。创建全局认证守卫。
    - **Expected:** 使用有效账号密码调用登录 API 返回 tokens。受保护端点（如 `GET /api/users/me`）需携带 token 才能访问。
- [ ] **Step 1.4: 实现用户信息修改 API**
    - **Run:** 编写并实现 `PATCH /api/users/me`，支持修改昵称、头像（可先处理 URL）、密码（需验证原密码）。更新 `users` 表的 `updated_at`。
    - **Expected:** 通过认证的用户能成功更新自己的信息，并返回更新后的用户数据。

## 2. Task 2: 核心笔记 CRUD 与数据模型
**Files:**
- Create: `backend/src/notes/notes.module.ts`, `backend/src/notes/notes.service.ts`, `backend/src/notes/notes.controller.ts`, `backend/src/notes/dto/*.ts`
- Create: `backend/src/notebooks/notebooks.module.ts`, `backend/src/notebooks/*.ts`
- Create: `backend/src/tags/tags.module.ts`, `backend/src/tags/*.ts`
- Modify: 数据库实体/模型定义（如 `backend/prisma/schema.prisma` 或 `backend/src/entities/*.ts`）
- Test: `backend/test/notes.e2e-spec.ts`

- [ ] **Step 2.1: 实现笔记本与标签的创建/删除 API**
    - **Run:** 编写并实现 `POST /api/notebooks`、`DELETE /api/notebooks/:id`、`POST /api/tags`（手动标签）、`DELETE /api/tags/:id`。确保用户隔离和唯一性约束（如 `UNIQUE(user_id, name)`）。
    - **Expected:** 用户能创建、删除属于自己的笔记本和标签。尝试创建同名笔记本返回 409 冲突。
- [ ] **Step 2.2: 实现笔记创建与获取 API**
    - **Run:** 编写并实现 `POST /api/notes` 和 `GET /api/notes`。`POST` 需支持关联 `notebookId`，生成 `content_hash`（如 SHA-256），并初始化 `version` 为 1。`GET` 需支持分页和按笔记本/标签过滤。
    - **Expected:** 创建的笔记能正确关联用户和笔记本，并返回 `NoteResponse` 结构（AI 相关字段可先为空）。列表 API 能返回对应用户的笔记。
- [ ] **Step 2.3: 实现笔记更新与删除 API**
    - **Run:** 编写并实现 `PATCH /api/notes/:id` 和 `DELETE /api/notes/:id`。更新时需重新计算 `content_hash` 并递增 `version`。
    - **Expected:** 笔记内容、标题、所属笔记本可更新。删除后，关联的 `note_tags` 记录应级联删除。

## 3. Task 3: AI 服务集成（异步处理）
**Files:**
- Create: `backend/src/ai/ai.module.ts`, `backend/src/ai/ai.service.ts`, `backend/src/ai/ai.controller.ts`, `backend/src/ai/dto/*.ts`
- Create: `backend/src/workers/ai.processor.ts` (RabbitMQ 消费者)
- Modify: `backend/src/notes/notes.service.ts` (调用 AI 服务)
- Test: `backend/test/ai.e2e-spec.ts`, `backend/test/workers/ai.processor.spec.ts`

- [ ] **Step 3.1: 搭建 AI 处理异步任务队列**
    - **Run:** 配置 RabbitMQ 连接，创建 `AiProcessor` 消费者类，监听 `ai.process` 队列。模拟 AI 处理逻辑：从内容中提取关键词作为标签，检测 `#` 生成大纲，随机返回相关笔记 ID。
    - **Expected:** 能向队列发送任务，并被 `AiProcessor` 消费处理。
- [ ] **Step 3.2: 实现笔记保存后触发 AI 处理**
    - **Run:** 在 `NotesService` 的 `create` 和 `update` 方法成功后，向 RabbitMQ 队列发布一个 `ProcessNoteRequest` 任务。确保内容不为空时才触发。
    - **Expected:** 创建/更新笔记后，能在 RabbitMQ 管理界面看到新任务。
- [ ] **Step 3.3: 实现 AI 处理结果写入与 API**
    - **Run:** 在 `AiProcessor` 中，处理完成后，将生成的标签（自动标签 `is_auto_generated=true`）插入 `tags` 和 `note_tags` 表，大纲 JSON 和关联笔记 ID 可暂存于 Redis 或更新笔记的扩展字段。实现 `GET /api/notes/:id` 时包含这些 AI 数据。
    - **Expected:** 调用 `GET /api/notes/:id` 返回的数据包含 `tags`（区分自动/手动）、`outline`、`relatedNotes` 字段。

## 4. Task 4: 多端实时同步与冲突解决
**Files:**
- Create: `backend/src/sync/sync.module.ts`, `backend/src/sync/sync.service.ts`, `backend/src/sync/sync.controller.ts`, `backend/src/sync/dto/*.ts`, `backend/src/sync/websocket.gateway.ts`
- Create: `backend/src/entities/sync-operation.entity.ts`, `backend/src/entities/conflict-backup.entity.ts`
- Modify: `backend/src/notes/notes.service.ts` (冲突检测)
- Test: `backend/test/sync.e2e-spec.ts`

- [ ] **Step 4.1: 实现同步操作记录表与推送 API**
    - **Run:** 实现 `POST /api/sync/push`。接收 `SyncPushRequest`，将 `operations` 存入 `sync_operations` 表，并立即处理（或标记待处理）这些操作，应用变更到主数据表（notes, notebooks, tags）。
    - **Expected:** 客户端推送的增删改操作能正确反映到数据库。
- [ ] **Step 4.2: 实现基于时间戳的冲突检测与解决**
    - **Run:** 在 `SyncService` 处理 `update` 操作时，比较请求中的 `timestamp` 与数据库中笔记的 `last_modified_at`。若请求更早，则判定为冲突。按规则将较早版本内容插入 `conflict_backups` 表，并返回冲突信息 `backupId`。
    - **Expected:** 模拟 TS 中的冲突示例，API 返回 `conflicts` 数组包含 `backupId`，且冲突备份表生成记录。
- [ ] **Step 4.3: 实现 WebSocket 实时同步通知**
    - **Run:** 创建 `WebSocketGateway`，在用户连接时认证。当某用户的笔记被其他设备更新后，通过 WebSocket 向该用户所有在线连接广播笔记 ID 和变更类型。
    - **Expected:** 在两个浏览器标签页登录同一用户，在一端修改笔记，另一端能在 2 秒内收到更新通知。

## 5. Task 5: 访客体验与限制功能
**Files:**
- Create: `backend/src/guest/guest.module.ts`, `backend/src/guest/guest.service.ts`, `backend/src/guest/guest.controller.ts`
- Modify: `backend/src/auth/auth.service.ts` (创建临时访客用户)
- Test: `backend/test/guest.e2e-spec.ts`

- [ ] **Step 5.1: 实现访客会话管理**
    - **Run:** 在 `AuthService` 中实现 `createGuest` 方法，创建 `is_guest=true` 且 `guest_expires_at` 为未来几小时的用户，并返回 tokens。实现中间件或守卫，对访客 token 进行特殊处理。
    - **Expected:** 调用 `POST /api/auth/guest`（需新增）能获得一个访客 token。
- [ ] **Step 5.2: 实现受限的语音转写与 OCR API**
    - **Run:** 实现 `POST /api/guest/speech-to-text` 和 `POST /api/guest/ocr`。使用 Redis 记录访客 ID 的使用次数/时长。集成模拟的 ASR/OCR 服务（可返回固定文本）。当超限时，返回错误码 `4401`。
    - **Expected:** 访客 token 调用语音转写，前 3 分钟成功，后续调用返回 403 及提示信息。OCR 同理限制 3 次。
- [ ] **Step 5.3: 实现访客前端路由与拦截**
    - **Run:** 在前端创建访客首页，仅展示语音和 OCR 体验入口。配置路由守卫，访客访问核心功能（如笔记列表）时重定向至体验页。
    - **Expected:** 未登录或使用访客 token 访问应用，只能看到体验功能，无法进入主笔记界面。

## 6. Task 6: 前端核心页面与状态管理
**Files:**
- Create: `frontend/app/(auth)/login/page.tsx`, `frontend/app/(auth)/register/page.tsx`
- Create: `frontend/app/(main)/page.tsx` (笔记列表页), `frontend/app/(main)/notes/[id]/page.tsx` (编辑/详情页), `frontend/app/(main)/settings/page.tsx`
- Create: `frontend/components/NoteEditor.tsx`, `frontend/components/Sidebar.tsx`, `frontend/components/SearchBar.tsx`
- Create: `frontend/lib/store/noteStore.ts` (Zustand 或 Context)
- Create: `frontend/hooks/useWebSocket.ts`, `frontend/hooks/useAutoSave.ts`
- Test: `frontend/__tests__/NoteEditor.test.tsx`

- [ ] **Step 6.1: 实现认证页面与全局状态**
    - **Run:** 实现登录、注册页面表单，调用后端 API。使用 Zustand 或 Context 创建全局 Auth Store 管理用户状态和 token。
    - **Expected:** 能成功登录/注册，并跳转到主页面。刷新页面后用户状态保持。
- [ ] **Step 6.2: 实现笔记列表页与侧边栏**
    - **Run:** 实现主页布局：左侧笔记本/标签树，顶部搜索框，中央笔记列表。从后端获取数据并渲染。实现新建笔记按钮。
    - **Expected:** 页面正确显示用户的笔记本、标签和笔记列表，点击可进入笔记详情。
- [ ] **Step 6.3: 实现富文本笔记编辑器与自动保存**
    - **Run:** 集成富文本编辑器（如 TipTap）。实现 `useAutoSave` hook，在编辑后 200ms 调用 `PATCH /api/notes/:id`。实现工具栏按钮触发语音录制和图片上传（先模拟）。
    - **Expected:** 编辑笔记时，内容自动保存到后端。控制台可看到定时请求。
- [ ] **Step 6.4: 集成 AI 侧边栏与 WebSocket 同步**
    - **Run:** 在笔记详情页，从 `GET /api/notes/:id` 获取并渲染大纲、标签、关联笔记。使用 `useWebSocket` hook 连接并监听笔记更新事件，收到后更新本地状态。
    - **Expected:** 打开一篇已处理的笔记，右侧显示 AI 生成的标签和相关笔记。在另一设备修改此笔记，本页面内容自动更新。

## 7. Task 7: 搜索功能与第三方服务模拟
**Files:**
- Create: `backend/src/search/search.module.ts`, `backend/src/search/search.service.ts`, `backend/src/search/search.controller.ts`
- Modify: `frontend/components/SearchBar.tsx` (集成语义搜索)
- Create: `frontend/app/api/search/route.ts` (可选，用于前端代理)
- Test: `backend/test/search.e2e-spec.ts`

- [ ] **Step 7.1: 实现关键词全文搜索**
    - **Run:** 在 `SearchService` 中实现 `GET /api/search?q=keyword`。使用 PostgreSQL 的全文搜索功能（`tsvector`/`tsquery`）对 `notes` 表的 `title` 和 `content` 进行检索。
    - **Expected:** 输入关键词能返回匹配的笔记列表。
- [ ] **Step 7.2: 模拟语义搜索**
    - **Run:** 实现 `GET /api/search/semantic?q=自然语言问题`。可先使用简单算法（如关键词提取后与笔记内容计算 Jaccard 相似度）返回相关笔记。
    - **Expected:** 输入自然语言句子，能返回一个按相关度排序的笔记列表。
- [ ] **Step 7.3: 前端搜索框集成**
    - **Run:** 在 `SearchBar` 组件中，调用搜索 API，并展示结果下拉列表。区分关键词搜索和语义搜索的触发方式（如按钮切换或自动检测）。
    - **Expected:** 在顶部搜索框输入内容，能实时（或点击后）展示搜索结果。

## 8. Task 8: 输入功能集成（语音、图片）与优化
**Files:**
- Create: `frontend/components/VoiceRecorder.tsx`, `frontend/components/ImageUploader.tsx`
- Modify: `backend/src/ai/ai.service.ts` (集成模拟 ASR/OCR)
- Create: `frontend/lib/utils/audioUtils.ts`

- [ ] **Step 8.1: 实现前端语音录制与实时转写模拟**
    - **Run:** 在 `VoiceRecorder` 组件中，使用 `MediaRecorder API` 录制音频。录制时，模拟流式返回文本（固定句子加随机词）。实现 60 分钟自动停止。低置信度部分高亮。
    - **Expected:** 点击录音按钮，编辑器内实时显示模拟转写文本，并有高亮提示。
- [ ] **Step 8.2: 实现图片上传与 OCR 模拟**
    - **Run:** 在 `ImageUploader` 组件中，处理文件选择，验证大小（10MB）。调用 `POST /api/guest/ocr`（或后续的正式 API）并模拟返回 OCR 文本，包含低置信度高亮。
    - **Expected:** 选择图片后，OCR 文本插入编辑器光标处，低置信度部分有视觉标记。
- [ ] **Step 8.3: 连接后端 AI 服务（替换模拟）**
    - **Run:** 将 `AiService` 中的标签提取、大纲生成、关联推荐逻辑替换为调用真实的 NLP 服务（如 OpenAI API、本地模型）。将语音转写和 OCR 请求转发至真实的 ASR/OCR 服务（如 Azure Cognitive Services）。
    - **Expected:** AI 功能（标签、大纲、推荐、转写、OCR）使用真实服务，效果更准确。

## 全局验收标准（完成本计划前必须全部满足）
- [ ] **业务功能完整：** 用户可完成注册、登录、创建/编辑/删除笔记和笔记本，使用语音转写和图片 OCR 输入，查看 AI 生成的标签、大纲和相关推荐，进行关键词和语义搜索。
- [ ] **多端同步与冲突解决：** 笔记的增删改能在不同浏览器标签页间通过 WebSocket 在 2 秒内同步。离线编辑后联网，能正确检测冲突并以最后修改时间为准解决，生成冲突备份。
- [ ] **访客体验受限：** 未登录用户只能访问体验页面，语音和 OCR 功能有次数/时长限制，超限后提示注册。
- [ ] **API 符合规范：** 所有后端 API 的请求/响应格式、HTTP 状态码、错误码（如 4001, 4101）符合 TS 定义。
- [ ] **数据持久化与安全：** 用户密码加密存储。所有数据操作（包括 AI 生成的标签）均正确持久化至 PostgreSQL。用户只能访问和操作自己的数据。
- [ ] **前端状态与交互：** 前端路由、认证状态、笔记数据管理正常。编辑器支持富文本与 Markdown，自动保存工作正常。
- [ ] **性能与体验：** 自动保存延迟约 200ms。AI 处理为异步，不阻塞主线程。大图片上传、长语音录制有前端验证。
- [ ] **测试覆盖：** 核心后端 API（认证、笔记 CRUD、同步、AI、访客）有通过的 E2E 测试。前端关键组件（如编辑器）有基础单元测试。
- [ ] **部署与运维：** 项目可通过 `docker-compose up` 一键启动所有服务（后端、前端、DB、Redis、RabbitMQ）。环境变量配置清晰。