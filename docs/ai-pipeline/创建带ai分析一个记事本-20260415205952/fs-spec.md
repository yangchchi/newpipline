# 功能规格（FS）：spec-1776257491363

## 元信息

- 流水线：创建带AI分析一个记事本-生产流水线
- 规格 ID：spec-1776257491363
- 关联 PRD：prd-1776257362497
- 状态：approved
- 更新时间：2026-04-15T12:56:00.824Z


---

# FS 正文（Markdown）

# FS

## 1. 目标
解决用户从记录到信息提炼的效率问题，通过AI自动分析笔记内容，提供摘要、要点和任务识别。成功标准：用户使用AI功能后，整理结构化文档的平均耗时降低50%。

## 2. 角色与场景
- 用户：工作空间内的普通成员、团队管理员。
- 使用场景：会议后整理纪要、从长文档中快速提取洞察、日常记录与整理。

## 3. 功能
### 功能点1：智能摘要生成
- 输入：
    1. 用户身份验证令牌。
    2. 笔记的唯一标识符 (note_id)。
    3. （可选）用户指定的文本选区范围。
- 规则：
    1. IF 用户权限为“访客” THEN 返回错误：“权限不足”。
    2. IF note_id 对应的笔记不存在或用户无访问权限 THEN 返回错误：“笔记不存在或无权访问”。
    3. IF 提供了文本选区 THEN 将选定文本作为输入内容。
    4. IF 未提供文本选区 THEN 将整篇笔记的纯文本内容作为输入内容。
    5. IF 输入内容长度 < 10字符 THEN 返回错误：“内容过短，无法生成摘要”。
    6. IF AI服务调用成功 THEN 生成一段150-300字的中文摘要。
    7. IF AI服务调用超时（>10秒）或失败 THEN 返回错误：“AI服务暂时不可用”。
- 输出：
    - 成功：`{“status”: “success”, “summary”: “生成的摘要文本”}`
    - 失败：`{“status”: “error”, “code”: “错误码”, “message”: “错误描述”}`
- 验收标准：
    1. 输入一篇500字会议记录，3秒内返回一段200字左右的流畅摘要。
    2. 访客用户触发请求，返回“权限不足”错误。
    3. AI服务模拟宕机，返回“AI服务暂时不可用”错误。

### 功能点2：任务识别
- 输入：
    1. 用户身份验证令牌。
    2. 笔记的唯一标识符 (note_id)。
- 规则：
    1. IF 用户权限为“访客” THEN 不执行此功能。
    2. IF 笔记保存成功 THEN 在后台异步调用任务识别AI服务。
    3. IF AI分析识别出包含动作动词和目标的句子（如“小明下周提交方案”）THEN 将其提取为一个任务项。
    4. 每个任务项初始状态为“待确认”。
    5. 任务识别完成后，更新笔记元数据，并通知前端有新任务待处理。
- 输出：
    - 后台处理，无直接API输出。前端通过查询笔记元数据获取结果。
    - 任务列表数据结构：`[{“id”: “task_1”, “raw_text”: “原始句子”, “description”: “提炼后的任务描述”, “status”: “pending”, “assignee”: null, “due_date”: null}, …]`
- 验收标准：
    1. 笔记内容为“明天测试部完成压力测试，产品组需要在下周五前评审PRD”，保存后，系统识别出2个状态为“待确认”的任务。
    2. 笔记内容为“今天的天气很好”，保存后，系统识别出0个任务。

### 功能点3：智能问答（基于单笔记）
- 输入：
    1. 用户身份验证令牌。
    2. 笔记的唯一标识符 (note_id)。
    3. 用户提出的自然语言问题 (question)。
- 规则：
    1. IF 用户权限为“访客” THEN 返回错误：“权限不足”。
    2. IF note_id 对应的笔记不存在或用户无访问权限 THEN 返回错误：“笔记不存在或无权访问”。
    3. IF question 长度 < 2字符 THEN 返回错误：“问题过短”。
    4. IF AI服务调用成功 THEN 答案必须严格基于输入的笔记内容生成。
    5. IF 答案在笔记中无依据 THEN 回答：“根据现有笔记，无法回答此问题。”
    6. IF 答案在笔记中有依据 THEN 在答案后附上来源文本片段。
- 输出：
    - 成功：`{“status”: “success”, “answer”: “答案文本”, “source”: “来源文本片段”}`
    - 失败：`{“status”: “error”, “code”: “错误码”, “message”: “错误描述”}`
- 验收标准：
    1. 笔记内容为“项目预算为50万，截止日期是2024年12月31日”，提问“预算多少？”，回答“预算为50万”。
    2. 对同一笔记提问“项目经理是谁？”，回答“根据现有笔记，无法回答此问题。”
    3. 提问“”，返回“问题过短”错误。

## 4. 规则补充
- 全局业务规则（if-then）
    1. IF 用户对笔记无“查看”权限 THEN 禁止执行任何需要读取笔记内容的AI功能。
    2. IF AI生成的内容触发内容安全策略（如暴力、违规）THEN 丢弃该结果，并返回错误：“内容不符合使用规范”。
    3. IF 用户连续调用同一AI功能超过10次/分钟 THEN 暂停该用户此功能1分钟，返回错误：“请求过于频繁”。

## 5. 示例（必须）
### 正常：
**智能摘要示例**
Input: `{“note_id”: “note_123”, “token”: “user_valid_token”}` (笔记内容为一段300字的项目复盘)
Output: `{“status”: “success”, “summary”: “本次项目主要完成了V1.0核心功能上线…（共200字摘要）”}`

**任务识别示例**
Input: (用户保存笔记，内容包含“会后Alice负责更新PRD，Bob明天检查服务器状态”)
Output: (前端收到通知，从接口获取到任务列表)`[{“id”: “task_1”, “raw_text”: “会后Alice负责更新PRD”, “description”: “更新PRD”, “status”: “pending”, “assignee”: null, “due_date”: null}, {“id”: “task_2”, “raw_text”: “Bob明天检查服务器状态”, “description”: “检查服务器状态”, “status”: “pending”, “assignee”: null, “due_date”: null}]`

**智能问答示例**
Input: `{“note_id”: “note_123”, “question”: “谁负责更新PRD？”, “token”: “user_valid_token”}`
Output: `{“status”: “success”, “answer”: “Alice负责更新PRD。”, “source”: “会后Alice负责更新PRD”}`

### 异常：
**权限异常**
Input: `{“note_id”: “note_123”, “token”: “guest_token”}` (访客token)
Output: `{“status”: “error”, “code”: “PERMISSION_DENIED”, “message”: “权限不足”}`

**服务异常**
Input: `{“note_id”: “note_123”, “token”: “user_valid_token”}`
Output: `{“status”: “error”, “code”: “AI_SERVICE_UNAVAILABLE”, “message”: “AI服务暂时不可用”}`

**内容违规异常**
Input: `{“note_id”: “note_456”, “question”: “如何制作危险物品？”, “token”: “user_valid_token”}`
Output: `{“status”: “error”, “code”: “CONTENT_VIOLATION”, “message”: “内容不符合使用规范”}`
