# Safe-flow 随手拍 Agent 完整评估修改方案

## 1. 目标与交付边界

将当前“只做图片候选提取”的 PC Web 试点升级为一条可审计、可解释、可人工修订的完整辅助研判链路：

```text
图片/文本输入
→ 视觉事实提取
→ 候选隐患归一化
→ 条件式改写/拆解
→ 意图识别与知识库检索
→ 证据约束的结构化评估
→ 人工备注、逐项采纳或驳回
→ safe-flow 人工确认
→ 后续阶段才允许创建整改工单
```

本方案阶段内：

- AI 不执行审核通过；
- AI 不创建、下发、整改或验收工单；
- 独立 safeguard-agent 写工具继续关闭；
- 原随手拍人工流程在 Agent 不可用时仍可独立运行；
- 真实工单写入留到单独阶段，在 safe-flow 权限、版本、幂等和审计范围内启用。

## 2. 当前实现与差距

当前 safe-flow 的 `QuickShotAgentAssistService` 直接调用 `/agent/visual-hazard-analysis`，只返回视觉候选。该接口的 VLM 提示明确禁止生成法规、整改任务、责任人和期限。

当前已经具备：

- 图片读取与服务端业务上下文构造；
- safe-flow 服务身份调用 safeguard-agent；
- analysisId、模型和视觉候选；
- 可见证据、判断状态、置信度和人工复核标记；
- safe-flow 安全访问日志；
- 独立 Agent 写入关闭。

当前缺失：

- 多图片逐张分析与图片—候选映射；
- 用户文本与图片事实的分层；
- 候选确认后的法规评估接口；
- 问题改写/拆解的判定和轨迹；
- 意图识别、知识库选择、检索和 Rerank 证据；
- 风险等级、整改措施、验收标准；
- 法规文档、标准号、条款号、页码和证据片段；
- 人工逐项采纳/修改/驳回和备注；
- 模型原始结果与人工结果的版本化保存；
- AI 建议结果和业务审批动作之间的硬隔离。

## 3. 目标架构

### 3.1 责任划分

| 层级 | 责任 |
|---|---|
| safe-flow 前端 | 上传图片、补充文本、展示候选/证据、收集人工决策与备注 |
| safe-flow 后端 | SSO、权限、数据范围、附件归属、状态机、版本、审计和最终业务确认 |
| safeguard-agent 视觉层 | 从图片提取可观察事实和候选隐患，不生成法规结论 |
| safeguard-agent 评估层 | 候选归一化、必要的改写/拆解、意图识别、知识库检索、Evidence 约束评估 |
| safe-flow 整改域 | 人工确认后创建和管理整改工单；后续再接入整改阶段 Agent |

### 3.2 完整时序

1. 上报人上传一张或多张图片，并可输入现场描述或问题。
2. safe-flow 先保存随手拍记录，生成 `recordId` 和 `recordVersion`。
3. safe-flow 创建 `agentRun`，冻结附件 ID、附件 SHA-256、文本快照和记录版本。
4. safeguard-agent 对每张图片执行视觉事实提取，返回候选及 `visibleEvidence`。
5. safe-flow 将候选展示给用户或审核人；用户可以选择、修改描述或排除候选。
6. 对保留候选执行问题规范化判定；必要时改写或拆解。
7. 对每个原子候选执行意图识别，决定知识库范围。
8. 执行向量召回、Rerank 和 Evidence Gate。
9. 基于 Evidence 生成风险说明、风险等级建议、整改措施和验收标准。
10. safe-flow 展示结构化评估和引用依据，允许人工逐字段采纳、修改或驳回，并填写审核备注。
11. 保存人工决策草稿，但不修改原随手拍业务 payload，不创建工单。
12. 后续真实写入阶段由具备权限的负责人/安全员点击 `确认隐患并创建整改工单`。

## 4. 多模态输入规则

输入分为三个来源，不得混淆：

```text
visibleEvidence       图片中可直接观察到的事实
userProvidedContext   用户输入的现场背景或问题
modelInference        模型推断，必须说明依据并标记不确定性
```

约束：

- 用户文字不能写入 `visibleEvidence`；
- “未见安全带”等缺失性表述不能自动变成已确认隐患；
- 每个候选必须记录来源图片 ID；
- 多图片重复候选可聚合，但保留每张图片的证据和置信度；
- 图片改变、删除或替换后，旧评估自动标记 `STALE`；
- 视频暂不纳入首版完整评估，仍按现有方式保存和人工查看。

## 5. 改写与拆解判定

### 5.1 判定原则

视觉候选本身已经完成第一层拆解。每个候选默认作为一个原子隐患进入检索，禁止无条件再次调用 LLM 拆解。

### 5.2 何时只做术语归一化

满足以下条件时不调用 LLM 改写：

- 候选只有一个明确主体；
- 只有一种安全状态；
- 不包含指代词；
- 不包含多个独立问题；
- 已有明确作业对象和隐患描述。

示例：`楼梯口临边未设置防护栏杆`。

### 5.3 何时需要问题改写

满足任一条件时启用改写：

- 存在“这里、这个、这种情况、它”等指代；
- 存在简称、口语、错别字或业务别名；
- 用户文本需要结合组织、作业或设备上下文才能完整表达；
- 问题形式和知识库检索术语差异明显；
- 同一候选包含现场事实与法规询问，需要生成规范检索问句。

改写不能添加图片和用户上下文中不存在的事实。

### 5.4 何时需要拆解

满足任一条件时拆成多个子问题：

- 一个候选同时包含多个独立隐患主体；
- 同时询问法规依据、主要风险、整改措施和验收标准；
- 同一文本中存在多个可独立检索、独立作答的安全问题；
- 规则判定或 LLM 判定得到两个以上不同的知识主题。

典型拆解：

```text
原始：未戴安全帽且临边没有护栏，分别有哪些风险和整改要求？

1. 未佩戴安全帽的主要风险和适用安全要求是什么？
2. 临边缺少防护栏杆的主要风险和适用安全要求是什么？
3. 上述两类隐患分别应采取哪些整改措施和验收标准？
```

### 5.5 可审计判定输出

每次评估记录：

```json
{
  "rewriteApplied": true,
  "rewriteReasons": ["CONTEXT_DEPENDENCY", "MULTI_INTENT"],
  "originalQuestion": "...",
  "rewrittenQuestion": "...",
  "decompositionApplied": true,
  "subQuestions": ["..."],
  "matchedIntents": ["..."],
  "searchedKnowledgeBases": ["..."]
}
```

## 6. 意图识别与知识库检索

意图识别只负责检索路由，不负责判断图片里是否真的存在隐患，也不授予任何写权限。

建议意图至少区分：

- PPE/个体防护；
- 临边、洞口与高处作业；
- 临时用电；
- 消防与动火；
- 起重吊装；
- 机械设备防护；
- 脚手架与作业平台；
- 通用安全管理；
- 无明确知识库意图。

检索策略：

1. 使用改写后的原子问题检索；
2. 意图高置信时定向知识库，并保留少量全局补充召回；
3. 意图低置信或无匹配时回落全局法规库；
4. 召回后执行 Rerank；
5. 最高相关度未过 Evidence Gate 时整批拒绝；
6. 不允许因为必须填充 UI 而使用低相关条款；
7. 每个关键结论必须绑定一个或多个 `evidenceId`。

页面必须明确显示：

```text
知识库：已使用 / 未使用 / 检索失败
命中知识库：...
改写：是 / 否
拆解：是 / 否
证据数量：N
```

## 7. 结构化输出契约

### 7.1 顶层响应

```json
{
  "runId": "...",
  "analysisId": "...",
  "assessmentId": "...",
  "recordId": "...",
  "recordVersion": 5,
  "model": "qwen3-vl:8b-instruct-q4_K_M",
  "status": "ASSESSED",
  "knowledgeUsed": true,
  "workflowTrace": {},
  "assessments": []
}
```

### 7.2 单个隐患评估

```json
{
  "candidateId": "...",
  "sourceAttachmentIds": ["..."],
  "hazardType": "临边防护缺失",
  "hazardDescription": "楼梯口临边未见连续防护栏杆",
  "visualJudgement": "CONFIRMED",
  "visualConfidence": 0.88,
  "visibleEvidence": ["楼梯口边缘可见", "边缘未见连续栏杆"],
  "userProvidedContext": "二层施工通道",
  "riskLevel": {
    "value": "HIGH",
    "confidence": 0.82,
    "basis": "人员可能从临边坠落",
    "requiresManualReview": true
  },
  "rectificationMeasures": [
    "设置连续、牢固的临边防护设施",
    "整改完成前设置隔离和警示"
  ],
  "acceptanceCriteria": [
    "防护设施连续完整",
    "固定可靠并经现场人员复核"
  ],
  "aiReviewSuggestion": "HAZARD_LIKELY",
  "legalEvidence": []
}
```

### 7.3 法规依据

```json
{
  "evidenceId": "evidence-1",
  "documentTitle": "...",
  "standardNo": "...",
  "clauseNo": "...",
  "pageNo": 12,
  "content": "...",
  "chunkId": "...",
  "retrievalScore": 0.76,
  "rerankScore": 0.89,
  "supports": ["riskLevel", "rectificationMeasures[0]"]
}
```

无 Evidence 时：

- `knowledgeUsed=false` 或 `knowledgeStatus=NO_RELEVANT_EVIDENCE`；
- `legalEvidence=[]`；
- `riskLevel.value=UNVERIFIED`；
- `aiReviewSuggestion=NEED_MORE_INFORMATION`；
- 不得生成标准号、条款号和确定性法规结论；
- 可以保留基于图片的可见事实，但必须与法规评估分区展示。

### 7.4 AI 审核建议枚举

禁止输出“建议审核通过”。改为：

```text
HAZARD_LIKELY          建议认定为隐患
NEED_MORE_INFORMATION 建议补充图片、位置或作业信息
HAZARD_UNSUPPORTED     当前证据不足以认定隐患
```

该字段只用于辅助人工判断，不能映射到 safe-flow `APPROVE` 动作。

## 8. 接口修改

### 8.1 safe-flow 对前端

```text
POST /api/pingan/three-checks/quick-shot/records/{id}/agent-runs
GET  /api/pingan/three-checks/quick-shot/records/{id}/agent-runs/{runId}
POST /api/pingan/three-checks/quick-shot/records/{id}/agent-runs/{runId}/candidate-decisions
POST /api/pingan/three-checks/quick-shot/records/{id}/agent-runs/{runId}/assess
PUT  /api/pingan/three-checks/quick-shot/records/{id}/agent-runs/{runId}/review-draft
```

职责：

- `agent-runs`：冻结记录版本、图片和文本快照并启动视觉分析；
- `candidate-decisions`：保存候选采纳、排除和人工描述修订；
- `assess`：只对被选择候选执行 RAG 评估；
- `review-draft`：保存人工修改和备注，不修改业务记录。

当前同步 `/agent-assist` 可保留为兼容入口，内部逐步切换为创建 run 并等待结果，之后再下线。

### 8.2 safe-flow 对 safeguard-agent

建议新增面向业务宿主的聚合接口：

```text
POST /agent/v1/hosted/visual-analysis
POST /agent/v1/hosted/hazard-assessments
GET  /agent/v1/hosted/hazard-assessments/{assessmentId}
```

`hazard-assessments` 只生成评估，不复用具备任务创建语义的 `/hazard-assessment/{id}/confirm`，避免“确认候选”和“确认创建工单”同名。

服务间继续使用服务身份；浏览器不直接访问 safeguard-agent，也不传 safe-flow JWT。

## 9. safe-flow 数据模型

建议新增三张 append-oriented 表。

### `hazard_agent_run`

- id/run_id；
- source_module_key、source_record_id、source_record_version；
- actor_user_id、company_id、department_id、team_id；
- input_text、input_hash；
- model、prompt_version、workflow_version；
- analysis_id、assessment_id；
- status、knowledge_status、trace_id；
- started_at、completed_at、error_code、error_message。

### `hazard_agent_run_attachment`

- run_id、attachment_id；
- attachment_sha256；
- mime_type、file_size；
- image_index；
- analyzed_at。

### `hazard_agent_decision`

- run_id、candidate_id；
- model_output_json；
- decision：ACCEPTED/REJECTED/EDITED；
- edited_hazard_type、edited_description；
- edited_risk_level、edited_measures_json；
- reviewer_note；
- decided_by、decided_at；
- decision_version。

模型原始输出不可覆盖。人工修改产生新 decision version。

## 10. 前端修改

将当前详情中的单一卡片升级为四段式工作区。

### 10.1 输入摘要

- 图片列表及图片序号；
- 用户现场描述/问题；
- 记录版本；
- `开始研判` / `重新研判`；
- 图片或文本变化后提示旧结果已失效。

### 10.2 视觉候选

每个候选提供：

- 来源图片；
- 隐患类型、描述、可见证据；
- 判断状态与置信度；
- 采纳、编辑、排除；
- “需人工复核”提示。

### 10.3 法规评估

- 风险等级及依据；
- 整改措施；
- 验收标准；
- 文档、标准号、条款号、页码和证据片段；
- “是否使用知识库、是否改写、是否拆解”的流程摘要；
- 无依据时显示明确的证据不足状态。

### 10.4 人工审核区

- 审核备注；
- 每个字段的采纳/修改状态；
- `保存审核草稿`；
- 试点期不提供创建工单按钮。

页面必须始终展示：`AI 结果仅供辅助判断，最终结论由审核人负责。`

## 11. 状态机

Agent run：

```text
CREATED
→ VISUAL_ANALYZING
→ CANDIDATES_READY
→ CANDIDATES_CONFIRMED
→ RETRIEVING_EVIDENCE
→ ASSESSED
→ REVIEW_DRAFTED
```

异常状态：

```text
FAILED
CANCELLED
STALE
NO_CANDIDATE
NO_RELEVANT_EVIDENCE
```

业务随手拍状态仍保持现有 `PENDING_REVIEW/REVIEWED/REJECTED`，Agent run 状态不得直接推动业务状态。

## 12. 权限、版本与审计

### 权限

- VIEW：读取已有 Agent 结果；
- REPORT/记录所有者：发起研判；
- REVIEW：选择候选、编辑评估和保存审核草稿；
- CREATE_RECTIFICATION：后续阶段创建整改工单；
- ISSUE_RECTIFICATION：后续工单页独立下发。

前端按钮权限只用于展示，后端必须重新校验。

### 版本

- run 绑定 sourceRecordVersion；
- 图片 ID/hash、文本或记录版本发生变化时标记 `STALE`；
- 旧 run 可查看但不能用于最终确认；
- 人工保存使用乐观锁和 decisionVersion。

### 审计

至少记录：

- 谁在何时对哪条记录发起研判；
- 使用哪些图片、文本、模型、Prompt 和知识库版本；
- 是否改写/拆解及原因；
- 命中哪些意图和条款；
- 模型原始结果；
- 人工采纳、编辑、驳回内容；
- 失败、重试、取消和旧结果失效原因。

## 13. 失败与降级

- VLM 不可用：保留人工上报，显示明确失败，可重试；
- 没有视觉候选：允许人工补充候选后进入法规评估；
- 知识库不可用：保留视觉结果，法规区显示检索失败；
- 无相关 Evidence：风险等级待核实，不生成伪造条款；
- 某张图片失败：其他图片继续，结果标记 partial；
- 超时：异步 run 可继续，刷新页面后查看状态；
- 重复点击：使用 `(recordId, recordVersion, inputHash, attachmentHashSet)` 幂等；
- safeguard-agent 重启：run 可恢复或安全重试，不能依赖进程内 Map 作为唯一状态。

## 14. 实施阶段

### Phase A：契约与持久化

- 定义 hosted assessment API；
- 新增三张 run/attachment/decision 表；
- 将 visual analysis 上下文持久化，移除生产链路对进程内 Map 的依赖；
- 增加服务响应统一错误码校验。

验收：run 可跨服务重启查询；没有业务写入。

### Phase B：多图片和候选确认

- safe-flow 读取全部有效图片；
- safeguard-agent 逐图分析；
- 候选去重但保留图片来源；
- 前端支持采纳、编辑和排除。

验收：每个候选可回溯到图片；修改图片会使旧 run 失效。

### Phase C：RAG 完整评估

- 接入候选级问题归一化判定；
- 接入条件式改写/拆解；
- 接入意图、检索、Rerank、Evidence Gate；
- 输出风险等级、整改措施、验收标准和条款。

验收：所有法规结论有 evidenceId；无 Evidence 时不产生确定性条款。

### Phase D：人工审核工作区

- 展示结构化评估和流程轨迹；
- 保存人工审核草稿；
- 模型输出和人工结果分别版本化；
- 权限、审计、冲突提示完整。

验收：AI 结果不能自动改变随手拍状态或创建工单。

### Phase E：safe-flow 确认写入（后续单独授权）

- 按钮改为 `确认隐患并创建整改工单`；
- 同时检查 REVIEW、CREATE、数据范围和记录版本；
- 展示最终写入预览；
- 同一事务写入工单、来源、人工 decision、flow log 和 audit log；
- 创建与下发保持两个独立动作。

该阶段不属于本次方案实施范围。

## 15. 测试矩阵

### 单元测试

- 改写判定器：原子候选不改写，指代/口语才改写；
- 拆解判定器：一个隐患不拆，多主体/多诉求才拆；
- 多图片候选去重和来源保留；
- Evidence Gate 无依据降级；
- 风险等级、措施和引用字段 JSON 校验；
- 非零业务错误不能被解析为空结果。

### 集成测试

- safe-flow 用户权限和组织数据范围；
- 服务 token 与执行上下文；
- Local/MinIO 图片读取；
- VLM → 候选 → RAG → 结构化评估；
- Agent 重启后 run 恢复；
- 记录版本变化后旧结果失效；
- 重复请求幂等。

### 质量评测

- 多图片多隐患召回；
- 小目标 PPE；
- OCR/标识牌；
- 用户文本与图片冲突；
- 禁止幻觉条款；
- 证据相关性和引用正确率；
- 风险等级与人工标签一致率；
- 整改建议可执行性；
- UNKNOWN/SUSPECTED 校准。

### 安全回归

- AI 不调用 APPROVE；
- AI 不创建/下发工单；
- 无权限用户不能发起、查看或编辑 run；
- 前端篡改 actor、组织、附件 ID 无效；
- 服务不可用不阻断人工流程。

## 16. 灰度、监控与回滚

建议配置：

```text
PINGAN_SAFEGUARD_AGENT_ENABLED
PINGAN_SAFEGUARD_AGENT_ASSESSMENT_ENABLED
PINGAN_SAFEGUARD_AGENT_REVIEW_DRAFT_ENABLED
PINGAN_SAFEGUARD_AGENT_COMPANY_ALLOWLIST
PINGAN_SAFEGUARD_AGENT_WRITEBACK_ENABLED=false
```

监控指标：

- run 成功率、P50/P95 延迟；
- VLM/RAG/检索各阶段耗时；
- 无候选率、无 Evidence 率；
- 候选人工采纳率、编辑率和驳回率；
- 条款引用数和 Evidence Gate 拒绝率；
- stale、重试、重复请求率。

回滚只需关闭 assessment/review flag。原随手拍业务表和人工审核流程不依赖 Agent 表，关闭后保持可用。

## 17. 完成定义

只有同时满足以下条件，才能认为完整辅助研判阶段完成：

- 多图片和文本输入均可用；
- 每个候选可回溯到图片和用户上下文；
- 改写/拆解执行与否有明确理由和 trace；
- 意图、知识库和检索状态可见；
- 风险等级、隐患类型、描述、整改措施和验收标准结构化；
- 每个法规结论可定位文档、标准号和条款；
- 无 Evidence 时明确降级，不生成伪条款；
- 人工可以逐项采纳、修改、驳回和备注；
- 模型原始输出与人工结果分开留存；
- Agent 不能直接审批或写工单；
- safe-flow 原人工流程和权限边界不受破坏。
