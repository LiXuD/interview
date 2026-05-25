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

目标：根据岗位画像和候选人摘要生成 5 道测评面试题。

输入来源：

- `InterviewTarget.title`：目标岗位名称。
- `InterviewTarget.jd`：岗位 JD。
- `CandidateProfile.summary`：候选人摘要。
- `CandidateProfile.skills`：候选人技能。
- `CandidateProfile.projects`：候选人项目经历。

System Prompt 要求：

- 角色：AI 技术面试教练。
- 必须生成恰好 5 道题。
- 难度分布：2 道基础题、2 道应用题、1 道深度题。
- 返回 JSON 仅含 `questions` 数组。

输出结构：

```json
{
  "questions": [
    "请简述你在项目中使用 Spring Boot 处理高并发请求的经验。",
    "如何设计一个支持百万级用户的 API 系统？",
    "Redis 在你的项目中扮演了哪些角色？",
    "请描述一次线上故障排查经历。",
    "如何保证分布式系统中的数据一致性？"
  ]
}
```

字段规则：

- `questions` 必须恰好 5 个元素。
- 每个元素非空字符串。

后端校验逻辑（`AiStructuredOutputService.validateQuestions`）：

- `questions` 非 null 且 `size() == 5`。
- 每个 question 非空非 blank。

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
  "nextTrainingTasks": ["系统设计容量规划"]
}
```

字段规则：

- `mockInterviewId` 必须与面试 ID 一致。
- `overallScore` 0 到 100。
- `dimensionScores` 非空数组，每项 `name` 和 `reason` 非空，`score` 0 到 100。
- `summary` 非空。
- `strengths`、`weaknesses`、`improvedAnswers`、`nextTrainingTasks` 均为非 null 数组。

后端校验逻辑（`AiStructuredOutputService.validateMockInterviewReport`）：

- `mockInterviewId` 匹配。
- `overallScore` 在 [0, 100]。
- `dimensionScores` 非空，每项校验 `name`、`score`、`reason`。
- `summary` 非空。
- 所有数组字段非 null。

## 10. 测试要求

- 每个 AI task 必须有结构化输出解析测试。
- 每个 AI task 必须覆盖非法 JSON 和缺失必填字段。
- 包含目标 ID 或会话 ID 的 task 还必须覆盖 ID 不匹配场景。
- 默认测试不得依赖 live AI 调用。
- 涉及简历原文的测试必须确认原文不落库、不写日志、不进入返回给 iOS 的字段。
- `candidateProfileDraft` 测试必须确认 `rawTextLength` 由后端计算，不受 AI 输出影响。
