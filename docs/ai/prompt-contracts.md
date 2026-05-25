# AI Prompt 契约

本文档定义 InterviewCoach 后端 AI 任务的输入边界、输出结构和失败策略。所有 AI 调用必须由后端统一代理，iOS 禁止直接调用 AI，也禁止解析 AI 原始文本。

## 1. 通用约束

- AI 原始响应只能停留在 `backend/src/main/java/com/interviewcoach/ai` 模块内部。
- 后端必须将 AI 原始 JSON 解析为强类型 DTO 后再返回给 iOS。
- 所有返回给 iOS 的 JSON 字段必须使用 camelCase。
- AI 输出解析失败时，后端最多重试 1 次；仍失败必须返回统一错误响应 `AI_PARSE_FAILED`。
- 用户 Provider 调用失败时，禁止自动切换平台 AI，必须让用户确认。
- Anthropic Provider 不进入当前实现范围。

## 2. Task: `candidateProfileDraft`

目标：根据用户临时上传的简历原文和项目经历生成候选人摘要草稿。

输入来源：

- `resumeText`：用户在 iOS 粘贴的简历原文，可为空。
- `projectRawText`：用户在 iOS 补充的项目经历原文，可为空。
- 原文只允许在本次请求内存中使用，不得落 PostgreSQL、Redis、文件、缓存或审计表。

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

## 3. Existing AI Tasks

| task | 输入边界 | 输出结构 |
|------|----------|----------|
| `candidateProfileDraft` | 简历原文、项目经历原文（内存使用，不落库） | `CandidateProfileDraftDto` |
| `jobBrief` | 目标岗位、JD、已确认 CandidateProfile 摘要 | `JobBriefDto` |
| `assessmentQuestions` | 目标岗位、岗位画像、候选人摘要 | `{ "questions": string[5] }` |
| `assessmentResult` | 5 题测评问题与用户回答 | `AssessmentResultDto` |
| `trainingPlan` | 测评短板、岗位画像、候选人摘要 | `{ "tasks": [{ "title": string, "description": string }] }` |
| `trainingFeedback` | 训练任务和用户回答 | `TrainingFeedbackDto` |
| `mockInterviewQuestion` | 目标岗位、候选人摘要、最近最多 6 轮上下文 | `{ "question": string }` |
| `mockInterviewReport` | 模拟面试最近上下文和目标岗位 | `MockInterviewReportDto` |

## 4. Prompt 编写要求

- Prompt 必须要求模型只返回合法 JSON，不返回 Markdown、解释文字或代码块。
- Prompt 必须明确目标 DTO 的字段名和字段类型。
- 对依赖 JD、岗位画像或用户摘要的 task，Prompt 必须提醒模型只基于已提供上下文生成内容，不得编造用户未提供的经历。
- MockInterview 追问 Prompt 最多携带最近 6 轮，也就是 12 条 message。
- Report 类输出必须围绕面试教练复盘，不生成招聘投递、题库社区、订阅付费或语音面试相关内容。

## 5. 测试要求

- 每个新增 AI task 必须有结构化输出解析测试。
- 每个新增 AI task 必须覆盖非法 JSON 和缺失必填字段；包含目标 ID 或会话 ID 的 task 还必须覆盖 ID 不匹配场景。
- 默认测试不得依赖 live AI 调用。
- 涉及简历原文的测试必须确认原文不落库、不写日志、不进入返回给 iOS 的字段。
