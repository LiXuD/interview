# AI Prompt 契约

本文档定义 InterviewCoach 后端 AI 任务的输入边界、输出结构和失败策略。所有 AI 调用必须由后端统一代理，iOS 禁止直接调用 AI，也禁止解析 AI 原始文本。

## 1. 通用约束

- AI 原始响应只能停留在 `backend/src/main/java/com/interviewcoach/ai` 模块内部。
- 后端必须将 AI 原始 JSON 解析为强类型 DTO 后再返回给 iOS。
- 所有返回给 iOS 的 JSON 字段必须使用 camelCase。
- AI 输出解析失败时，后端最多重试 1 次；仍失败必须返回统一错误响应 `AI_PARSE_FAILED`。
- 用户 Provider 调用失败时，禁止自动切换平台 AI，必须让用户确认。
- Anthropic Provider 不进入当前实现范围。
- 所有分数范围为 0 到 100，`confidence` 范围为 0 到 1。
- Prompt 必须要求模型只返回合法 JSON，不返回 Markdown、解释文字或代码块。
- Prompt 必须明确 DTO 字段名和字段类型。
- 依赖用户摘要或 JD 的 task，Prompt 必须提醒模型只基于已提供上下文，不得编造未提供的经历。
- 禁止要求、保存或返回 AI hidden chain-of-thought；如需解释依据，只能返回结构化的题目意图、rubric、评分理由、证据引用和记忆摘要。
- 教练记忆必须区分 `confirmed`、`observed`、`corrected`、`inferred`、`rejected` 来源；`inferred` 只能用于追问验证，`rejected` 禁止再次作为事实使用。

## 2. Task: `candidateProfileDraft`

目标：根据用户临时上传的简历原文和项目经历生成候选人摘要草稿。

输入来源：

- `resumeText`：用户在 iOS 粘贴的简历原文，可为空。
- `projectRawText`：用户在 iOS 补充的项目经历原文，可为空。
- 原文只允许在本次请求内存中使用，不得落 PostgreSQL、Redis、文件、缓存或审计表。

System Prompt 要求：

- 角色：简历摘要助手。
- 必须指定 `summary`（中文）、`skills`（数组）、`projects`（数组）、`experience`（数组）的字段类型。
- 必须要求只返回 JSON。
- 必须禁止编造候选人未提供的项目或技术。

输出 DTO：`CandidateProfileDraftDto`

```json
{
  "summary": "候选人的技术背景、岗位相关经验和面试准备重点摘要",
  "skills": ["Java", "Spring Boot", "SQL"],
  "projects": ["支付系统改造", "风控数据看板"],
  "experience": ["3 年 Java 后端开发经验"],
  "rawTextLength": 1234
}
```

字段规则：

- `summary` 必须是中文自然语言摘要，不能为空。
- `skills`、`projects`、`experience` 必须是数组，允许为空数组。
- `rawTextLength` 由后端根据原文计算，不依赖 AI 输出。
- 后端禁止把 `resumeText`、`projectRawText`、`rawResume`、`originalText` 或任何原文片段写入日志。

后端校验逻辑（`AiStructuredOutputService.validateCandidateProfileDraft`）：

- `summary` 非空非 blank。
- `skills`、`projects`、`experience` 非 null（允许空数组）。

## 3. Task: `jobBrief`

目标：根据目标岗位 JD 和已确认的候选人摘要生成岗位画像。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `InterviewTarget.jd`：岗位 JD 原文。
- `CandidateProfile.summary`：已确认的候选人摘要。
- `CandidateProfile.skills`：已确认的技能列表。
- `CandidateProfile.projects`：已确认的项目经历列表。
- `CandidateProfile.experience`：已确认的工作经验列表。

System Prompt 要求：

- 角色：AI 技术面试教练。
- 必须指定 `importance` 枚举值（required/important/bonus）。
- 必须指定 `userLevel` 枚举值（unknown/weak/basic/solid/strong）。
- 必须指定 `confidence` 范围 0 到 1。
- 禁止编造候选人未确认的经历。

输出 DTO：`JobBriefDto`

```json
{
  "targetId": "uuid-string",
  "roleSummary": "岗位核心职责摘要",
  "skillMap": [
    {
      "name": "Java",
      "importance": "required",
      "userLevel": "unknown",
      "gap": "需要确认技术深度"
    }
  ],
  "mustHaveSkills": ["Java", "Spring Boot"],
  "niceToHaveSkills": ["Redis", "Kafka"],
  "businessContext": ["业务场景描述"],
  "interviewTopics": ["面试重点话题"],
  "candidateMatch": ["匹配点"],
  "riskAreas": ["风险点"],
  "confidence": 0.8
}
```

字段规则：

- `targetId` 必须与传入的目标 ID 一致。
- `roleSummary` 非空。
- `skillMap`、`mustHaveSkills`、`niceToHaveSkills`、`businessContext`、`interviewTopics`、`candidateMatch`、`riskAreas` 均为非 null 数组。
- `skillMap` 每项的 `name` 和 `gap` 非空，`importance` 必须在 allowed 枚举中，`userLevel` 必须在 allowed 枚举中。
- `confidence` 0 到 1。

后端校验逻辑（`AiStructuredOutputService.validateJobBrief`）：

- `targetId` 匹配。
- `roleSummary` 非空。
- 所有数组字段非 null。
- `skillMap` 逐项校验 `name`、`importance`、`userLevel`、`gap`。
- `confidence` 在 [0, 1]。

## 4. Task: `assessmentQuestions`

目标：根据岗位画像和候选人摘要生成 5 道结构化测评面试题。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `InterviewTarget.jd`：岗位 JD。
- `CandidateProfile.summary`：候选人摘要。
- `CandidateProfile.skills`：候选人技能。
- `CandidateProfile.projects`：候选人项目经历。

System Prompt 要求：

- 角色：AI 技术面试教练。
- 必须生成恰好 5 道题。
- 每道题包含 `question`、`dimension`、`difficulty`、`intent`、`rubric`。
- 难度分布：2 道 basic、2 道 medium、1 道 deep。
- 固定能力维度：`technicalDepth`、`projectSpecificity`、`systemThinking`、`tradeoffAwareness`、`failureHandling`、`communicationClarity`、`businessContext`。
- 返回 JSON 仅含 `questions` 数组。

输出结构：

```json
{
  "questions": [
    {
      "question": "请结合你的支付项目说明一次 Redis 缓存一致性问题。",
      "dimension": "systemThinking",
      "difficulty": "medium",
      "intent": "验证候选人是否能把缓存一致性问题讲到业务约束和技术权衡。",
      "rubric": ["说明业务场景", "解释一致性策略", "讲清权衡和结果"]
    }
  ]
}
```

字段规则：

- `questions` 必须恰好 5 个元素。
- 每项 `question`、`intent` 非空。
- `dimension` 必须为 7 个固定维度之一。
- `difficulty` 必须为 `basic`、`medium` 或 `deep`。
- `rubric` 非空数组，每项非空字符串。

后端校验逻辑（`AiStructuredOutputService.validateQuestions`）：

- `questions` 非 null 且 `size() == 5`。
- 每项 `question`、`intent` 非空非 blank。
- `dimension` 在允许枚举中。
- `difficulty` 在允许枚举中。
- `rubric` 非 null 且非空，每项非空。

## 5. Task: `assessmentResult`

目标：对用户的 5 道测评回答进行评分。

输入来源：

- `AssessmentSession.id`：测评会话 ID。
- `AssessmentSession.questions`：5 道题目。
- `AssessmentSession.answers`：用户的 5 条回答。

System Prompt 要求：

- 角色：AI 技术面试教练，评分用户回答。
- `assessmentId` 必须与传入的测评 ID 一致。
- `totalScore` 范围 0-100。
- `dimensions` 每项 `score` 范围 0-100。
- 必须包含 `strengths`、`weaknesses`、`nextActions` 数组。

输出 DTO：`AssessmentResultDto`

```json
{
  "assessmentId": "uuid-string",
  "totalScore": 72,
  "dimensions": [
    {
      "name": "Java 基础",
      "score": 75,
      "reason": "对核心概念理解扎实"
    }
  ],
  "strengths": ["Java 基础扎实"],
  "weaknesses": ["系统设计需要加强"],
  "nextActions": ["复习容量规划方法"]
}
```

字段规则：

- `assessmentId` 必须与测评会话 ID 一致。
- `totalScore` 0 到 100。
- `dimensions` 非空数组，每项 `name` 和 `reason` 非空，`score` 0 到 100。
- `strengths`、`weaknesses`、`nextActions` 均为非 null 数组。

后端校验逻辑（`AiStructuredOutputService.validateAssessmentResult`）：

- `assessmentId` 匹配。
- `totalScore` 在 [0, 100]。
- `dimensions` 非空，每项校验 `name`、`score`、`reason`。
- `strengths`、`weaknesses`、`nextActions` 非 null。

## 6. Task: `trainingPlan`

目标：根据测评短板生成 1 天训练计划，包含 2-4 个任务。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `AssessmentResult.weaknesses`：测评发现的短板。
- `AssessmentResult.nextActions`：测评建议的下一步行动。

System Prompt 要求：

- 角色：AI 技术面试教练。
- 生成 2-4 个训练任务。
- 每个任务需针对具体短板。
- 返回 JSON 含 `tasks` 数组，每项有 `title` 和 `description`。

输出结构：

```json
{
  "tasks": [
    {
      "title": "系统设计容量规划练习",
      "description": "针对系统设计短板，练习如何估算 QPS 和存储需求。"
    }
  ]
}
```

字段规则：

- `tasks` 包含 2 到 4 个元素。
- 每项 `title` 和 `description` 非空。

后端校验逻辑（`AiStructuredOutputService.validateTrainingPlan`）：

- `tasks` 非 null 且 `size()` 在 [2, 4]。
- 每项 `title` 和 `description` 非空。

## 7. Task: `trainingFeedback`

目标：对用户提交的训练任务回答进行评分和反馈。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `TrainingTask.id`：训练任务 ID。
- `TrainingTask.title`：训练任务标题。
- `TrainingTask.description`：训练任务描述。
- 用户提交的回答文本。

System Prompt 要求：

- 角色：AI 技术面试教练，评分用户回答。
- `taskId` 必须与传入的训练任务 ID 一致。
- `score` 范围 0-100。
- 必须包含 `feedback`、`rewrittenAnswer`、`followUpQuestion` 非空文本。
- 必须包含 `problems`、`recommendedReviewPoints` 数组。

输出 DTO：`TrainingFeedbackDto`

```json
{
  "taskId": "uuid-string",
  "score": 75,
  "feedback": "回答覆盖了基本概念，但深度不够。",
  "problems": ["缺少量化数据"],
  "rewrittenAnswer": "改进后的回答示范",
  "followUpQuestion": "相关追问",
  "recommendedReviewPoints": ["复习要点1"]
}
```

字段规则：

- `taskId` 必须与训练任务 ID 一致。
- `score` 0 到 100。
- `feedback`、`rewrittenAnswer`、`followUpQuestion` 非空。
- `problems`、`recommendedReviewPoints` 非 null 数组。

后端校验逻辑（`AiStructuredOutputService.validateTrainingFeedback`）：

- `taskId` 必须与传入的训练任务 ID 一致。
- `score` 在 [0, 100]。
- `feedback`、`rewrittenAnswer`、`followUpQuestion` 非空。
- `problems`、`recommendedReviewPoints` 非 null。

## 8. Task: `mockInterviewQuestion`

目标：生成模拟面试的下一个问题（开场或追问）。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `InterviewTarget.jd`：岗位 JD（仅开场时传入）。
- `CandidateProfile.summary`：候选人摘要（仅开场时传入）。
- `CandidateProfile.skills`：候选人技能（仅开场时传入）。
- `CandidateProfile.projects`：候选人项目经历（仅开场时传入）。
- 最近最多 6 轮对话历史（仅追问时传入）。

System Prompt 要求：

- 角色：AI 面试官，进行技术模拟面试。
- 每次只问一个问题。
- 追问必须基于候选人上一条回答。
- 保持专业和对话性。

输出结构：

```json
{
  "question": "请介绍一下你在项目中使用 Spring Boot 的经验。"
}
```

字段规则：

- `question` 非空字符串。

后端校验逻辑（`AiStructuredOutputService.validateMockInterviewQuestion`）：

- `question` 非空非 blank。

## 9. Task: `mockInterviewReport`

目标：对完整的模拟面试进行复盘评估，生成面试报告。

输入来源：

- `MockInterview.id`：模拟面试 ID。
- `InterviewTarget.title`：目标岗位名称。
- 最近最多 6 轮对话历史。

System Prompt 要求：

- 角色：AI 技术面试教练，评估模拟面试。
- `mockInterviewId` 必须与传入的面试 ID 一致。
- `overallScore` 范围 0-100。
- `dimensionScores` 每项 `score` 范围 0-100。
- 输出必须围绕面试教练复盘，不涉及招聘投递、题库社区、订阅付费或语音面试。

输出 DTO：`MockInterviewReportDto`

```json
{
  "mockInterviewId": "uuid-string",
  "overallScore": 76,
  "dimensionScores": [
    {
      "name": "技术深度",
      "score": 78,
      "reason": "能结合项目说明核心技术的使用"
    }
  ],
  "summary": "整体表现中等偏上",
  "strengths": ["项目经验丰富"],
  "weaknesses": ["系统设计需要加强"],
  "improvedAnswers": ["改进后的回答示范"],
  "likelyFollowUpPoints": ["真实面试中最可能继续追问的点"],
  "nextTrainingTasks": ["系统设计容量规划"]
}
```

字段规则：

- `mockInterviewId` 必须与面试 ID 一致。
- `overallScore` 0 到 100。
- `dimensionScores` 非空数组，每项 `name` 和 `reason` 非空，`score` 0 到 100。
- `summary` 非空。
- `strengths`、`weaknesses`、`improvedAnswers`、`likelyFollowUpPoints`、`nextTrainingTasks` 均为非 null 数组。

后端校验逻辑（`AiStructuredOutputService.validateMockInterviewReport`）：

- `mockInterviewId` 匹配。
- `overallScore` 在 [0, 100]。
- `dimensionScores` 非空，每项校验 `name`、`score`、`reason`。
- `summary` 非空。
- 所有数组字段非 null。

## 10. Task: `coachingMemory`

目标：根据会话数据（测评、训练、模拟面试）生成结构化教练记忆，沉淀用户能力画像。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- 测评：`AssessmentResultDto`（总分、维度、强项、短板、下一步行动）和题目列表。
- 训练：`TrainingFeedbackDto`（评分、反馈、问题、改进示范、复习要点）和任务标题。
- 模拟面试：`MockInterviewReportDto`（总分、维度、总结、强项、短板、改进示范、训练建议）。

System Prompt 要求：

- 角色：AI 技术面试教练。
- 只基于本次会话数据生成记忆，不得编造未提供的信息。
- 返回 JSON 仅含记忆字段，不含 targetId、sourceType 等（由后端补充）。

输出 DTO：`CoachingMemoryDto`

```json
{
  "observedStrengths": [{"content": "能讲清项目背景", "source": "observed", "confidence": "high"}],
  "observedWeaknesses": [{"content": "容量估算缺少数字依据", "source": "observed", "confidence": "medium"}],
  "recurringProblems": [{"content": "回答偏概念，缺少业务指标", "source": "observed", "confidence": "high"}],
  "verifiedExperience": [{"content": "用户确认做过支付回调链路优化", "source": "confirmed", "confidence": "high"}],
  "unverifiedClaims": [{"content": "提到高并发优化，但未给出指标", "source": "inferred", "confidence": "low"}],
  "recommendedNextFocus": [{"content": "容量规划", "source": "observed", "confidence": "high"}],
  "avoidRepeating": [{"content": "不要继续问泛泛的 Redis 基础概念", "source": "observed", "confidence": "medium"}]
}
```

字段规则：

- 所有字段均为非 null 对象数组（允许空数组）。
- 每个记忆对象必须包含 `content`、`source`、`confidence`。
- `source` 只能为 `confirmed`、`observed`、`corrected`、`inferred`、`rejected`。
- `confidence` 只能为 `high`、`medium`、`low`。

后端校验逻辑（`AiStructuredOutputService.validateCoachingMemory`）：

- 所有 7 个数组字段非 null。
- 每个记忆对象的 `content` 非空，`source` 和 `confidence` 在允许枚举中。

## 11. Post-MVP Real AI Adaptive Coaching 计划契约

Task 18-25 将在现有 AI task 基础上扩展以下契约。实现前必须同步 `docs/api/openapi.yaml`、后端 DTO、iOS DTO 和本文件。

### 11.1 结构化测评题（已实现 Task 20）

`assessmentQuestions` 已升级为固定 5 题结构化输出（见上方 Section 4）：

```json
{
  "questions": [
    {
      "question": "请结合你的支付项目说明一次 Redis 缓存一致性问题。",
      "dimension": "systemThinking",
      "difficulty": "medium",
      "intent": "验证候选人是否能把缓存一致性问题讲到业务约束和技术权衡。",
      "rubric": ["说明业务场景", "解释一致性策略", "讲清权衡和结果"]
    }
  ]
}
```

固定能力维度：

- `technicalDepth`
- `projectSpecificity`
- `systemThinking`
- `tradeoffAwareness`
- `failureHandling`
- `communicationClarity`
- `businessContext`

### 11.2 逐题评分与回答结构诊断

Task 23 的测评结果必须支持逐题诊断。`assessmentQuestionScore` 任务的 AI 输出 JSON 结构：

```json
{
  "questionIndex": 0,
  "score": 75,
  "dimension": "technicalDepth",
  "feedback": "回答覆盖了基本概念，但在技术深度和量化表达方面仍需加强。",
  "problems": ["缺少具体的技术指标", "未说明性能优化的对比数据"],
  "improvedExample": "在项目中，我负责优化订单服务的接口性能。通过引入 Redis 缓存热点数据、优化 SQL 索引，将 P99 延迟从 800ms 降至 150ms。",
  "answerStructure": {
    "background": "present: 明确说明了项目背景和技术场景",
    "task": "partial: 任务描述较笼统",
    "action": "present: 详细说明了缓存和索引优化方案",
    "result": "missing: 未给出优化前后的具体数据对比",
    "tradeoff": "missing: 未讨论技术选型的权衡",
    "review": "missing: 未进行复盘反思"
  },
  "followUpRisks": ["面试官可能追问具体的性能数据对比", "面试官可能追问 Redis 缓存穿透如何处理"],
  "contentHighlights": ["技术方案选择合理", "排查思路清晰"],
  "contentGaps": ["缺少量化指标", "未说明技术权衡"]
}
```

字段约束：

- `answerStructure` 诊断 STAR+ 结构（背景 background、任务 task、行动 action、结果 result、权衡 tradeoff、复盘 review）。
- 每个 `answerStructure` 字段格式为 `"状态: 简短评语"`，状态只能是 `present`、`partial` 或 `missing`。
- `followUpRisks` 至少 1 条，列出真实面试官可能追问的薄弱点。
- `contentHighlights` 和 `contentGaps` 为非空列表。
- `improvedExample` 必须基于候选人已确认的真实经历改写，禁止编造新项目。
- 聚合总分、维度分、主要短板和下一步训练建议由 `assessmentResult` 任务在汇总所有逐题诊断后输出。

### 11.3 教练记忆生成

Task 21 的教练记忆必须由后端解析为强类型 DTO。记忆内容只允许包含结构化事实和观察：

```json
{
  "targetId": "uuid-string",
  "sourceType": "assessment",
  "observedStrengths": [{"content": "能讲清项目背景", "source": "observed", "confidence": "high"}],
  "observedWeaknesses": [{"content": "容量估算缺少数字依据", "source": "observed", "confidence": "medium"}],
  "recurringProblems": [{"content": "回答偏概念，缺少业务指标", "source": "observed", "confidence": "high"}],
  "verifiedExperience": [{"content": "用户确认做过支付回调链路优化", "source": "confirmed", "confidence": "high"}],
  "unverifiedClaims": [{"content": "提到高并发优化，但未给出指标", "source": "inferred", "confidence": "low"}],
  "recommendedNextFocus": [{"content": "容量规划", "source": "observed", "confidence": "high"}],
  "avoidRepeating": [{"content": "不要继续问泛泛的 Redis 基础概念", "source": "observed", "confidence": "medium"}]
}
```

### 11.4 用户纠错

Task 22 必须支持用户纠正 AI 判断。纠错后的内容在后续 Prompt 中优先级高于 AI 推断：

- `corrected`：用户修正后的事实或结论。
- `rejected`：用户否认的经历、短板或判断，禁止再次作为事实使用。
- `inferred`：AI 推断，只能作为追问验证材料。

后端提供教练记忆 item 级纠错入口：

- `PATCH /api/coaching-memories/{id}/corrections`
- 请求字段：`field`、`itemIndex`、`source`、`content`
- `source` 只允许 `corrected` 或 `rejected`
- 写入后的 item `confidence` 固定为 `high`
- 接口必须按当前登录用户查询记忆，禁止跨用户纠错

### 11.5 自适应专项训练

Task 24 的专项训练会话必须根据上一轮回答返回下一步动作：

```json
{
  "action": "continue",
  "question": "请补充这个方案在峰值流量下的容量估算。",
  "feedback": "回答已经覆盖整体方案，但缺少量化依据。",
  "score": 72,
  "memoryUpdates": ["容量估算仍需训练"]
}
```

`action` 允许值：

- `continue`：继续追问。
- `pass`：当前短板基本达标。
- `switch`：换一个相关角度。
- `stop`：用户明显卡住，先给讲解或结束本轮。

### 11.6 自适应模拟面试增强

Task 25 的模拟面试追问必须：

- 引用用户上一条回答中的具体内容。
- 根据历史短板和本轮表现决定追深、换维度、要求举例、要求量化结果或要求解释权衡。
- 继续遵守最近 6 轮、最多 12 条 message 的上下文限制。
- 结合教练记忆时只使用必要摘要，不传完整历史。

## 12. 测试要求

- 每个 AI task 必须有结构化输出解析测试。
- 每个 AI task 必须覆盖非法 JSON 和缺失必填字段。
- 包含目标 ID 或会话 ID 的 task 还必须覆盖 ID 不匹配场景。
- 默认测试不得依赖 live AI 调用。
- Task 18-25 的真实 AI 验收必须显式开启，不能污染默认 CI。
- 涉及简历原文的测试必须确认原文不落库、不写日志、不进入返回给 iOS 的字段。
- `candidateProfileDraft` 测试必须确认 `rawTextLength` 由后端计算，不受 AI 输出影响。
