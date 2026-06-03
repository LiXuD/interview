# 持续存在的 InterviewCoachAgent 设计

日期：2026-06-03

状态：已确认，待实施

## 1. 背景

当前产品已经具备岗位画像、结构化测评、教练记忆、自适应训练、多轮模拟面试、进步追踪和真实 AI 质量运营能力。用户完成一次业务动作时，各业务 Service 会分别决定何时调用 AI、如何构造 Prompt、是否更新 `CoachingMemory`。

这种架构能够完成业务闭环，但“教练”还不是系统中的一等实体：

- 测评、训练、模拟面试分别表现为独立 AI 功能。
- 用户当前目标、优先改善维度和下一步行动没有统一持久化状态。
- 业务模块各自调用 AI，容易产生重复推理和缺少统一目标的记忆更新。
- 用户无法持续感知“同一个教练正在理解我并带我向目标前进”。

本设计将产品升级为持续存在的面试教练 Agent。云端真实 AI 仍是 Agent 的推理引擎，不引入本地模型。

## 2. 目标

为每个用户的每个目标岗位建立一个持续存在的 `InterviewCoachAgent`：

```text
userId + targetId = 一个持续存在的 InterviewCoachAgent
```

Agent 跨越测评、训练、模拟面试和复盘生命周期，持续维护当前目标、重点能力维度、下一步建议和最近决策。业务模块成为 Agent 可调用的受控工具，而不是彼此独立的 AI 教练。

成功标准：

- 用户切换或重新打开 App 后，仍能看到同一个目标岗位下的教练状态。
- 完成测评、训练、模拟面试或记忆纠错后，Agent 会基于最新可信事实更新下一步建议。
- Agent 使用云端真实 AI 进行需要判断的决策，不使用本地模型或 stub 作为产品能力。
- Agent 不复制完整业务历史，不保存隐私原文或 hidden chain-of-thought。
- Agent 的工具调用、模型调用、失败、延迟和决策结果可观测、可测试、可限制。

## 3. 非目标

- 不实现通用聊天助手。
- 不实现无限自主循环或无边界工具调用。
- 不实现题库社区、课程系统、招聘投递、企业端、订阅付费、多人协作或语音面试。
- 不让 iOS 直接调用模型。
- 不引入本地模型。
- 不用 Agent 状态替代 `CoachingMemory`、`Progress`、Assessment、Training 或 MockInterview 的事实记录。
- 第一阶段不实现后台定时主动运行；后续可在事件驱动 Agent 稳定后单独评估。

## 4. 核心原则

### 4.1 Agent 是持续存在的业务实体

Agent 不是一次请求中的 Prompt，也不是对 `AiModelGateway` 的重新命名。它拥有独立身份、状态、生命周期和并发版本。

### 4.2 云端模型是推理引擎

Agent 在需要判断、规划、总结或选择下一步行动时通过后端 `AiModelGateway` 调用云端真实 AI。确定性检查和聚合由代码完成，不浪费模型调用。

### 4.3 受控 Agent，而不是无限自主 Agent

Agent 只能从白名单工具中选择动作，每次运行有模型调用次数、工具调用次数、超时和重试预算。模型不能自行创建未知工具或无限循环。

### 4.4 事实、记忆、状态分离

```text
业务实体 = 测评、训练、模拟面试等可追溯事实
CoachingMemory = 长期可信理解与纠错语义
Progress = 能力维度和趋势聚合
InterviewCoachAgent = 当前目标、重点和下一步行动决策
```

Agent 只引用必要摘要，不复制完整历史。

## 5. Agent 生命周期与数据模型

### 5.1 InterviewCoachAgent

建议后端新增 `agent` 模块，并持久化以下字段：

```text
InterviewCoachAgent
- id
- userId
- targetId
- status
- currentStage
- currentGoal
- activeFocusDimensions
- nextRecommendedAction
- lastEventType
- lastDecisionSummary
- lastRunAt
- version
- createdAt
- updatedAt
```

约束：

- `userId + targetId` 唯一。
- 所有查询必须按当前用户隔离。
- `version` 使用乐观锁，防止快速连续事件覆盖状态。
- `lastDecisionSummary` 只保存面向业务的结构化摘要，不保存 hidden chain-of-thought。
- 删除账号时 Agent 远端数据随账号删除。

### 5.2 Agent 状态

建议状态枚举：

```text
status:
- active
- paused
- completed

currentStage:
- targetSetup
- profileConfirmation
- assessment
- training
- mockInterview
- review
```

`currentStage` 描述用户当前所处阶段，`currentGoal` 描述教练当前希望帮助用户改善的具体目标，`activeFocusDimensions` 描述当前优先能力维度。

### 5.3 AgentDecision

云端模型必须返回后端可解析的强类型 DTO：

```text
AgentDecision
- currentGoal
- focusDimensions
- recommendedAction
- rationaleSummary
- toolCalls
- memoryUpdateRequired
- planAdjustmentRequired
```

`rationaleSummary` 是可展示或可审计的简短理由，不是模型思维链。

## 6. 事件驱动运行模型

第一阶段采用事件驱动 Agent。Agent 持续存在，但只在明确业务事件发生时运行。

建议事件：

```text
TARGET_CREATED
RESUME_SUMMARY_CONFIRMED
ASSESSMENT_COMPLETED
TRAINING_TASK_COMPLETED
TRAINING_SESSION_COMPLETED
MOCK_INTERVIEW_COMPLETED
MEMORY_CORRECTED
APP_SESSION_STARTED
```

运行流程：

```text
业务行为完成
-> 在同一业务事务中持久化幂等 CoachEvent
-> 业务事务提交后异步调度
-> InterviewCoachAgentRunner 加载 Agent
-> 加载必要的 CoachingMemory、Progress 与业务事实摘要
-> 代码执行确定性状态检查
-> 云端真实 AI 生成 AgentDecision
-> 校验结构化决策与工具白名单
-> 执行允许的工具或保存下一步建议
-> 保存 Agent 状态和决策记录
```

Agent 失败不能回滚已经完成的测评、训练或模拟面试事实。Agent 决策应允许独立重试。

`CoachEvent` 只保存用户、目标、事件类型、来源类型、来源 ID、幂等键和处理状态等低风险元数据，不保存用户回答原文、简历原文、Prompt 或 Completion。事件处理线程不依赖原始 HTTP 请求的 `SecurityContext`，必须根据事件所属用户建立临时受限执行上下文，确保 `AiModelGateway` 仍选择正确用户的云端 Provider，并在调用完成后清理上下文。

## 7. 受控工具

Agent 可调用的工具必须白名单化，并由后端实现稳定输入输出契约：

```text
AssessmentTool
TrainingPlanTool
AdaptiveTrainingTool
MockInterviewTool
ProgressAnalysisTool
CoachingMemoryTool
```

工具职责：

- `AssessmentTool`：读取测评状态或发起已有测评能力。
- `TrainingPlanTool`：生成或调整受控训练计划。
- `AdaptiveTrainingTool`：围绕当前短板推进训练会话。
- `MockInterviewTool`：发起或建议下一次模拟面试。
- `ProgressAnalysisTool`：读取结构化能力趋势。
- `CoachingMemoryTool`：读取必要可信摘要或请求记忆更新。

第一阶段不允许模型直接执行数据库写入、构造任意 HTTP 请求或访问未知工具。

受控工具编排最多允许两次模型决策：第一次决策可以请求有限数量的只读工具，第二次决策接收低风险结构化工具摘要并输出最终行动。第二次决策禁止继续请求工具，避免无限自主循环。

## 8. 与现有模块的关系

### 8.1 CoachingMemory

`CoachingMemory` 继续承载长期可信理解，保留 `confirmed`、`observed`、`corrected`、`inferred`、`rejected` 来源与可信度规则。

Agent 使用记忆时：

- 优先使用 `confirmed`、`observed`、`corrected`。
- `inferred` 只能作为待验证追问线索。
- `rejected` 禁止重新进入事实上下文。
- 禁止加载完整历史或简历原文。

### 8.2 Progress

`Progress` 继续聚合能力维度、趋势和证据来源。Agent 读取 Progress 决定当前重点，但不复制其历史分数。

### 8.3 业务 Service

现有 Assessment、Training、MockInterview Service 继续负责业务事实和可靠执行。迁移过程中，它们逐步从“各自决定下一步教练行为”转为“发布事件并作为 Agent 工具被编排”。

### 8.4 AiModelGateway

所有 Agent 模型调用仍通过后端 `AiModelGateway`，统一使用平台真实 AI 或用户配置的 OpenAI-compatible 云端 Provider。iOS 禁止直连模型。

## 9. 减少重复模型调用

Agent 的目标不是机械保证每个用户动作只调用一次模型，而是让每次调用服务于同一个持续目标，并消除无意识重复。

迁移方向：

- 将业务结果生成、下一步建议、是否更新记忆和是否调整计划放入同一个受控决策上下文。
- 复用已持久化的岗位画像、能力画像、教练记忆和进度摘要。
- 确定性检查由代码完成。
- 对低优先级事件进行合并，避免短时间内重复运行 Agent。
- 显式记录每次 Agent 运行的模型调用和工具调用预算。

## 10. API 与 iOS 体验

建议新增查询 API：

```text
GET /api/targets/{targetId}/coach-agent
```

返回强类型 Agent 状态：

```text
- status
- currentStage
- currentGoal
- activeFocusDimensions
- nextRecommendedAction
- lastDecisionSummary
- lastRunAt
```

iOS 新增 `Features/CoachAgent`，作为持续教练入口，展示：

- 当前教练目标。
- 当前重点能力维度。
- 下一步推荐行动。
- 最近一次教练判断摘要。
- 进入已有测评、训练、模拟面试或进步追踪的明确动作。

该入口不是聊天页，也不展示内部 Provider、Prompt、工具调用或监控指标。

## 11. 安全、隐私与可靠性

- Agent 数据按当前用户隔离，业务接口从 `SecurityUtils.currentUser()` 获取用户。
- Agent 不保存简历原文、用户回答原文、API Key、Authorization Header、完整请求头或 hidden chain-of-thought。
- Agent Observability 只记录低风险元数据，例如 event、task、provider、model、latency、outcome、retry、toolName。
- 所有模型输出必须由后端解析和校验，禁止把原始字符串返回给 iOS。
- 模拟面试仍限制最近 6 轮、最多 12 条 message。
- Agent 每次运行必须设置模型调用次数、工具调用次数、超时和重试上限。
- Agent 失败时保留已完成业务事实，并提供可重试状态。
- 产品能力验收必须显式使用真实 AI，stub 和 mock 只能验证工程结构。

## 12. 测试与验收

### 12.1 默认测试

- Agent 按 `userId + targetId` 唯一创建和查询。
- 用户不能访问其他用户的 Agent。
- 事件能够更新 Agent 状态。
- 乐观锁或幂等键防止重复事件覆盖状态。
- 工具白名单拒绝未知工具。
- Agent 失败不会回滚业务事实。
- DTO 使用 camelCase，iOS 不解析模型原始文本。
- 隐私字段不会写入 Agent 状态、日志或 metrics。

### 12.2 真实 AI 验收

- 显式加载 `backend/.env`。
- 验证完成测评后 Agent 能选择合理的训练方向。
- 验证完成训练后 Agent 能基于最新表现调整下一步建议。
- 验证完成模拟面试后 Agent 能识别重复短板和下一步行动。
- 验证记忆纠错后 `rejected` 内容不会再次作为事实。
- 验证 Agent 决策输出可稳定解析，失败能定位到具体 event、task 和 provider。
- 重要 Agent 行为变更必须运行完整真实 AI 质量评测或说明未运行原因。

## 13. 分阶段实施

Phase 4 按小任务逐步推进：

1. Agent 身份、状态和查询 API。
2. CoachEvent、AgentDecision 和事件运行器。
3. 测评、训练、模拟面试、记忆纠错事件接入。
4. 下一步推荐统一由 Agent 输出。
5. 白名单工具编排与调用预算。
6. 记忆更新和训练计划调整逐步收拢。
7. iOS 持续教练入口。
8. 真实 AI 回归、隐私与发布硬化。

每个任务必须保持已有业务闭环可运行，不一次性替换全部 AI 调用。
