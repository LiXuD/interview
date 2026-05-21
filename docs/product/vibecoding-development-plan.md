# AI 技术岗面试教练 App Vibecoding 三层开发计划

本文档是本项目后续 vibecoding 开发的主计划文档。所有实现任务必须同时遵守项目根目录的 `AGENTS.md` 和 `CLAUDE.md`。

## 0. 计划结论

本计划分为三层：

1. Product Spec：产品目标、MVP 边界、用户路径。
2. Architecture Spec：认证、数据流、AI Provider、隐私、DTO、Report 生命周期、目录边界。
3. Vibecoding Task Plan：按垂直切片拆任务，每个任务可单独实现、测试、验收。

MVP Provider 范围：

- 首版只做平台默认 AI。
- 首版支持 OpenAI-compatible 自定义 Provider。
- Anthropic 协议后移，不进入最窄 MVP。

---

## 1. Product Spec

### 1.1 产品目标

开发一款 iOS App：用户输入目标岗位、JD、简历或项目经历后，App 通过 AI 完成岗位研究、能力测评、专项训练、模拟面试和复盘报告。

第一版不是题库 App，而是 AI 面试教练。核心体验必须围绕一条闭环：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

### 1.2 MVP 成功标准

用户必须能完整走通：

1. 登录。
2. 创建目标岗位。
3. 粘贴 JD。
4. 粘贴简历或项目经历。
5. 明确同意临时上传原文生成摘要。
6. 确认服务端返回的 `CandidateProfile` 摘要。
7. 生成 `JobBrief` 岗位画像。
8. 完成 5 题 `Assessment`。
9. 查看 `Assessment Report`。
10. 生成 1 天 `TrainingPlan`。
11. 完成 1 个 `TrainingTask`。
12. 完成 1 次文字 `MockInterview`。
13. 查看 `MockInterview Report`。
14. 删除账号，本地和远端数据清理完成。

### 1.3 MVP 不做

MVP 禁止实现：

- 题库社区。
- 招聘投递。
- 企业端面试系统。
- 课程系统。
- 订阅付费。
- 多人协作。
- 语音面试。
- 简历自动投递。
- Anthropic 自定义 Provider。
- 任意模型厂商适配。
- 多天复杂训练计划。

---

## 2. Architecture Spec

### 2.1 技术栈

iOS：

- SwiftUI。
- iOS 17+。
- SwiftData。
- Keychain。
- async/await。
- URLSession。
- Codable DTO。
- MVVM 或轻量 feature-based 架构。

后端：

- Spring Boot 3。
- PostgreSQL。
- Redis。
- Spring Security。
- OpenAPI。
- 模块化单体。

AI：

- 后端统一代理。
- MVP 默认平台 AI。
- MVP 支持 OpenAI-compatible 自定义 Provider。
- Anthropic 作为 post-MVP Provider 扩展点预留，不实现。

### 2.2 项目目录结构

项目根目录采用 monorepo：

```text
interview/
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── .gitignore
├── docs/
│   ├── api/
│   │   └── openapi.yaml
│   ├── product/
│   │   ├── mvp-spec.md
│   │   └── vibecoding-development-plan.md
│   ├── architecture/
│   │   └── system-design.md
│   ├── privacy/
│   │   └── data-policy.md
│   └── ai/
│       ├── prompt-contracts.md
│       └── provider-contracts.md
├── ios/
│   └── InterviewCoach/
├── backend/
│   ├── build.gradle 或 pom.xml
│   └── src/
├── infra/
│   └── docker-compose.yml
└── scripts/
```

禁止在项目根目录随意创建业务代码文件。新增文件必须遵守 `CLAUDE.md` 中的目录与模块边界规范。

### 2.3 iOS 模块边界

`ios/InterviewCoach/` 下采用 feature-based 结构：

```text
InterviewCoach/
├── App/
├── Core/
│   ├── API/
│   │   └── DTO/
│   ├── Auth/
│   ├── Storage/
│   │   └── LocalModels/
│   ├── Security/
│   └── UI/
├── Features/
│   ├── Auth/
│   ├── Targets/
│   ├── Profiles/
│   ├── JobBrief/
│   ├── Assessment/
│   ├── Training/
│   ├── MockInterview/
│   ├── Reports/
│   └── Settings/
└── Resources/
```

规则：

- 网络 DTO 放在 `Core/API/DTO`，必须对应后端 camelCase JSON。
- SwiftData 本地模型放在 `Core/Storage/LocalModels`。
- 网络 DTO 和 SwiftData Model 禁止混用。
- 每个 Feature 内部可以有 `Views`、`ViewModels`、`Models`。
- iOS 禁止直接调用大模型，只能调用后端 API。

### 2.4 后端模块边界

后端包名统一使用：

```text
com.interviewcoach
```

Spring Boot 目录结构：

```text
backend/src/main/java/com/interviewcoach/
├── InterviewCoachApplication.java
├── common/
│   ├── api/
│   ├── error/
│   └── security/
├── auth/
├── user/
├── target/
├── profile/
├── ai/
├── jobbrief/
├── assessment/
├── training/
├── mockinterview/
└── report/
```

每个业务模块内部按需使用：

```text
controller/
service/
repository/
entity/
dto/
```

规则：

- Controller 只接收 Request DTO、返回 Response DTO。
- Service 承载业务逻辑。
- Repository 只负责数据访问。
- Entity 禁止直接返回给 iOS。
- AI 原始响应只能停留在 `ai` 模块内部，业务模块只能拿解析后的强类型 DTO。
- Spring Security 配置必须放在 `common/security` 或 `auth` 下，禁止散落在业务模块里。

### 2.5 认证与用户 API

开发阶段先做 dev login，TestFlight 前替换为 Sign in with Apple。

Auth API：

- `POST /api/auth/dev-login`
- `POST /api/auth/apple`
- `POST /api/auth/logout`
- `GET /api/me`
- `DELETE /api/me`

Token 规则：

- iOS 使用 Bearer Token。
- Token 存 Keychain。
- 所有业务 API 必须要求登录。
- `DELETE /api/me` 删除远端用户数据后，iOS 必须清空 SwiftData 和 Keychain。
- 后端必须使用 Spring Security 标准过滤器链解析 Bearer Token。
- 认证通过后，当前用户必须通过 `SecurityContextHolder` 获取。
- 禁止在业务接口里硬编码用户 ID 或绕过认证。

### 2.6 数据存储边界

纯本地 SwiftData：

- 简历原文。
- 用户未确认上传的项目经历原文。
- 本地草稿。
- 页面缓存状态。

远端 PostgreSQL：

- 用户确认后的 `CandidateProfile` 摘要。
- `InterviewTarget`。
- `JobBrief`。
- `AssessmentSession`。
- `TrainingPlan`。
- `MockInterview`。
- `Report`。
- 加密后的 OpenAI Provider API Key。

同步规则：

- App 启动后以远端业务数据为准同步。
- 简历原文永不落远端库。
- 用户删除账号后，远端数据删除，本地 SwiftData 和 Keychain 同步清空。

### 2.7 简历摘要隐私链路

MVP 不做 iOS 本地 AI 摘要。

摘要生成流程：

1. 用户在 iOS 粘贴简历或项目经历，原文只存本地。
2. 用户点击“生成摘要”前，App 明确提示：原文将临时发送到后端 AI 进行摘要生成，不会落库。
3. 后端 `POST /api/profiles/draft-summary` 接收原文。
4. 后端只在内存中使用原文调用 AI。
5. 后端不得保存原文，不得记录原文日志。
6. 后端返回 `CandidateProfileDraftDto`。
7. 用户在 iOS 编辑并确认摘要。
8. iOS 调用 `POST /api/profiles/confirm`。
9. 后端只保存确认后的 `CandidateProfile` 摘要。

日志强约束：

- `profiles/draft-summary` 的 Controller、Service、AI 调用适配层中，严禁将简历原文输出到 `System.out`、`System.err` 或任何日志框架。
- 禁止 `logger.info/debug/warn/error` 记录 `resumeText`、`rawResume`、`projectRawText`、`originalText` 或任何原文字段。
- 允许记录 requestId、userId、字符长度、处理耗时。

### 2.8 API JSON 约束

所有后端向 iOS 返回的 JSON 必须使用 camelCase 小驼峰。

强约束：

- 后端必须返回强类型 DTO。
- 禁止直接把 AI 原始字符串透传给 iOS。
- 禁止 iOS 解析 AI 原始文本。
- DTO 字段必须与 OpenAPI 一致。
- Swift `Codable` 字段必须与后端 camelCase JSON 对齐。

统一错误响应：

```json
{
  "code": "string",
  "message": "string",
  "requestId": "string"
}
```

### 2.9 AI Provider MVP 设计

MVP Provider：

- `platformDefault`
- `userOpenAICompatible`

OpenAI-compatible 配置：

- `name`
- `baseUrl`
- `apiKey`
- `model`
- `openaiApiMode`: `responses` 或 `chatCompletions`

API：

- `GET /api/ai-providers`
- `POST /api/ai-providers`
- `POST /api/ai-providers/test`
- `PATCH /api/ai-providers/{id}/default`
- `DELETE /api/ai-providers/{id}`

安全要求：

- API Key 后端加密保存。
- API Key 不返回 iOS。
- API Key 不写日志。
- Provider 调用失败，不自动切换平台 AI，必须用户确认。

### 2.10 核心业务 API

Target：

- `POST /api/targets`
- `GET /api/targets`
- `GET /api/targets/{id}`
- `PATCH /api/targets/{id}`
- `DELETE /api/targets/{id}`

Profile：

- `POST /api/profiles/draft-summary`
- `POST /api/profiles/confirm`
- `GET /api/profiles/current`

JobBrief：

- `POST /api/job-briefs/generate`
- `GET /api/job-briefs/{targetId}`

Assessment：

- `POST /api/assessments/start`
- `POST /api/assessments/{id}/answers`
- `POST /api/assessments/{id}/finish`
- `GET /api/assessments/{id}`

Training：

- `POST /api/training-plans/generate`
- `GET /api/training-plans/{targetId}`
- `POST /api/training-tasks/{id}/answer`
- `PATCH /api/training-tasks/{id}/complete`

MockInterview：

- `POST /api/mock-interviews/start`
- `POST /api/mock-interviews/{id}/answer`
- `POST /api/mock-interviews/{id}/finish`
- `GET /api/mock-interviews/{id}`

Report：

- `GET /api/reports/{id}`
- `GET /api/reports?targetId={targetId}`

### 2.11 AI JSON Schema 约束

所有 AI 结构化输出必须解析成 DTO。解析失败时，后端最多重试 1 次；仍失败则返回：

```json
{
  "code": "AI_PARSE_FAILED",
  "message": "AI returned invalid structured output.",
  "requestId": "req_xxx"
}
```

`JobBriefDto`：

```json
{
  "targetId": "string",
  "roleSummary": "string",
  "skillMap": [
    {
      "name": "string",
      "importance": "required",
      "userLevel": "unknown",
      "gap": "string"
    }
  ],
  "mustHaveSkills": ["string"],
  "niceToHaveSkills": ["string"],
  "businessContext": ["string"],
  "interviewTopics": ["string"],
  "candidateMatch": ["string"],
  "riskAreas": ["string"],
  "confidence": 0.85
}
```

枚举：

- `importance`: `required`, `important`, `bonus`
- `userLevel`: `unknown`, `weak`, `basic`, `solid`, `strong`
- `confidence`: 0 到 1

`AssessmentResultDto`：

```json
{
  "assessmentId": "string",
  "totalScore": 72,
  "dimensions": [
    {
      "name": "technicalDepth",
      "score": 70,
      "reason": "string"
    }
  ],
  "strengths": ["string"],
  "weaknesses": ["string"],
  "nextActions": ["string"]
}
```

分数范围：

- `totalScore`: 0 到 100
- `dimensions.score`: 0 到 100

`TrainingFeedbackDto`：

```json
{
  "taskId": "string",
  "score": 75,
  "feedback": "string",
  "problems": ["string"],
  "rewrittenAnswer": "string",
  "followUpQuestion": "string",
  "recommendedReviewPoints": ["string"]
}
```

`MockInterviewReportDto`：

```json
{
  "mockInterviewId": "string",
  "overallScore": 76,
  "dimensionScores": [
    {
      "name": "projectExplanation",
      "score": 78,
      "reason": "string"
    }
  ],
  "summary": "string",
  "strengths": ["string"],
  "weaknesses": ["string"],
  "improvedAnswers": ["string"],
  "nextTrainingTasks": ["string"]
}
```

### 2.12 Report 生命周期

Report 是统一复盘产物。

Report 来源：

- Assessment finish 会创建一个 `Report`，类型为 `assessment`。
- MockInterview finish 会创建一个 `Report`，类型为 `mockInterview`。
- TrainingTask answer 只生成 `TrainingFeedback`，不创建 Report。
- TrainingPlan 完成后，post-MVP 可生成阶段性 Report，MVP 不做。

关系：

- `AssessmentSession` 与 `Report`：一对一。
- `MockInterview` 与 `Report`：一对一。
- `InterviewTarget` 与 `Report`：一对多。

`Report.type`：

- `assessment`
- `mockInterview`

### 2.13 MockInterview 上下文限制

`POST /api/mock-interviews/{id}/answer` 组装 Prompt 时：

- 最多携带最近 6 轮对话。
- 也就是最多 12 条 message。
- 完整历史可以存 PostgreSQL。
- Redis 只缓存当前会话必要状态。
- 禁止每次把完整历史塞给模型。

---

## 3. Vibecoding Task Plan

### Task 1: Walking Skeleton

目标：打通 iOS -> 后端。

范围：

- iOS 启动页。
- 后端 `/actuator/health` 或 `/api/health`。
- iOS NetworkService 调用 health。
- 显示后端连接状态。

文件边界：

- `ios/`
- `backend/`
- `docs/api/openapi.yaml`

验收：

- App 启动成功。
- 后端启动成功。
- iOS 能显示 `Backend connected`。

### Task 2: OpenAPI 与基础 DTO

目标：先锁 API 契约。

范围：

- 建立 `docs/api/openapi.yaml`。
- 定义统一错误响应。
- 定义 camelCase DTO。
- iOS 创建对应 Codable 类型。

文件边界：

- `docs/api/openapi.yaml`
- iOS DTO。
- 后端 DTO。

验收：

- OpenAPI 可被解析。
- iOS DTO 字段与 JSON 示例一致。
- 后端 DTO 与 OpenAPI 字段一致。

### Task 3: Dev Login

目标：完成开发期登录闭环，并建立后续业务接口可复用的真实认证基础。

范围：

- `POST /api/auth/dev-login`
- `GET /api/me`
- Bearer Token 生成与校验。
- Spring Security Bearer Token 拦截器链。
- `SecurityContextHolder` 当前用户注入。
- Token 存 Keychain。
- iOS 显示当前用户。

强约束：

- 后端必须使用 Spring Security 建立标准 Bearer Token 拦截器链。
- `POST /api/auth/dev-login` 根据传入的虚拟用户名签发包含用户 ID 的 JWT 或等价 Token。
- 之后所有业务请求必须在 `Authorization: Bearer <token>` Header 中携带 Token。
- 业务接口必须从 `SecurityContextHolder` 获取当前用户。
- 不允许在 Controller 或 Service 中硬编码用户 ID。
- 不允许做虚假的越权绕过。
- 未认证请求访问业务接口必须返回 401。

文件边界：

- `backend/src/main/java/com/interviewcoach/auth`
- `backend/src/main/java/com/interviewcoach/common/security`
- `ios/InterviewCoach/InterviewCoach/Core/Auth`

验收：

- 未登录不能访问业务 API。
- 登录后可访问 `/api/me`。
- 删除或篡改 Bearer Token 后，请求返回 401。
- Task 4 可以直接复用当前认证机制，不需要重构安全配置。

### Task 4: Target CRUD

目标：岗位目标完整闭环。

范围：

- 后端 Target CRUD。
- iOS 创建、列表、详情、编辑、删除页面。
- SwiftData 缓存 Target。

文件边界：

- `backend/src/main/java/com/interviewcoach/target`
- `ios/InterviewCoach/InterviewCoach/Features/Targets`

验收：

- 创建“银行统一支付平台 Java 后端”目标。
- 重启 App 后目标仍可见。
- 删除后本地和远端都消失。
- 不同用户之间不能访问彼此的 Target。

### Task 5: CandidateProfile 隐私链路

目标：解决简历原文、临时上传、确认摘要边界。

范围：

- 本地保存简历原文。
- `POST /api/profiles/draft-summary`
- `POST /api/profiles/confirm`
- 不落库原文。
- iOS 展示摘要确认页。

强约束：

- `profiles/draft-summary` 的 Controller 和 Service 类中，严禁出现任何将简历原文打印至 `System.out`、`System.err` 或 Logback/SLF4J 日志框架的代码。
- 严禁写入 `logger.info`、`logger.debug`、`logger.warn`、`logger.error`，只要内容包含或可能包含 `resumeText`、`rawResume`、`projectRawText`、`originalText` 等原文字段都禁止。
- 允许记录 requestId、userId、字符长度、处理耗时。
- 原文只允许作为方法参数在内存中传递给摘要生成服务。
- 原文不得落 PostgreSQL、Redis、文件、缓存、审计表。
- 完成 Task 5 前必须双重确认：搜索 Controller、Service、AI adapter，确保没有原文日志输出代码。

文件边界：

- `backend/src/main/java/com/interviewcoach/profile`
- `ios/InterviewCoach/InterviewCoach/Features/Profiles`
- `docs/privacy`

验收：

- 未确认前远端没有 CandidateProfile。
- 确认后远端只有摘要。
- PostgreSQL 中不存在简历原文字段。
- 日志中没有简历原文或原文片段。
- 代码搜索找不到将原文字段传给 logger、System.out、System.err 的语句。

### Task 6: Platform AI + JobBrief

目标：第一个 AI 结构化输出完整跑通。

范围：

- 平台默认 AI。
- JobBrief Prompt。
- AI JSON 解析。
- `JobBriefDto` 返回 iOS。
- iOS 展示岗位画像。

文件边界：

- `backend/src/main/java/com/interviewcoach/jobbrief`
- `backend/src/main/java/com/interviewcoach/ai`
- `ios/InterviewCoach/InterviewCoach/Features/JobBrief`

验收：

- 能生成岗位画像。
- 后端返回 camelCase DTO。
- AI_PARSE_FAILED 有明确错误响应。

### Task 7: Assessment 5 题测评

目标：完成基准测评。

范围：

- 生成 5 道场景题。
- 用户逐题回答。
- finish 后生成 `AssessmentResultDto`。
- 创建 `Report(type=assessment)`。

文件边界：

- `backend/src/main/java/com/interviewcoach/assessment`
- `backend/src/main/java/com/interviewcoach/report`
- `ios/InterviewCoach/InterviewCoach/Features/Assessment`

验收：

- 完成 5 题后能看到评分。
- Report 可通过 `/api/reports/{id}` 查询。
- Report 与 AssessmentSession 为一对一。

### Task 8: TrainingPlan 1 天任务

目标：基于短板生成最小训练计划。

范围：

- `POST /api/training-plans/generate`
- 只生成 1 天计划。
- 每天 2-4 个任务。
- 支持任务完成状态。

文件边界：

- `backend/src/main/java/com/interviewcoach/training`
- `ios/InterviewCoach/InterviewCoach/Features/Training`

验收：

- 计划必须引用 Assessment 的 weakness。
- 完成任务后状态可保存。
- TrainingTask answer 只生成 TrainingFeedback，不创建 Report。

### Task 9: MockInterview

目标：完成一次文字模拟面试。

范围：

- start 创建会话。
- answer 返回追问。
- finish 生成 `MockInterviewReportDto`。
- 创建 `Report(type=mockInterview)`。
- 限制最近 6 轮上下文。

文件边界：

- `backend/src/main/java/com/interviewcoach/mockinterview`
- `backend/src/main/java/com/interviewcoach/report`
- `ios/InterviewCoach/InterviewCoach/Features/MockInterview`

验收：

- AI 追问基于上一条回答。
- 长对话不会无限增长 Prompt。
- finish 后能查看 Report。
- Report 与 MockInterview 为一对一。

### Task 10: User OpenAI Provider

目标：支持用户自定义 OpenAI-compatible Provider。

范围：

- Provider CRUD。
- API Key 加密保存。
- 连接测试。
- 设置默认 Provider。
- 业务 AI 调用走用户默认 Provider。

文件边界：

- `backend/src/main/java/com/interviewcoach/ai`
- `ios/InterviewCoach/InterviewCoach/Features/Settings`

验收：

- API Key 不返回 iOS。
- API Key 不写日志。
- 测试失败有明确错误。
- 删除 Provider 后不可再调用。

### Task 11: Delete Account

目标：完成账号删除和本地清理。

范围：

- `DELETE /api/me`
- 删除远端业务数据。
- iOS 清空 SwiftData。
- iOS 清空 Keychain。

文件边界：

- `backend/src/main/java/com/interviewcoach/user`
- `backend/src/main/java/com/interviewcoach/auth`
- `ios/InterviewCoach/InterviewCoach/Core/Auth`
- `ios/InterviewCoach/InterviewCoach/Core/Storage`

验收：

- 删除账号后重新打开 App 无历史数据。
- 远端无法查询旧数据。
- API Key、Report、Target、Profile、Assessment、Training、MockInterview 都被删除。

### Task 12: TestFlight Polish

目标：补齐可用性。

范围：

- 空状态。
- 加载状态。
- 错误提示。
- 隐私说明。
- 首次使用引导。
- 网络失败重试。

验收：

- 最窄 MVP 路径从头到尾无阻塞。
- 无明显崩溃。
- 用户知道什么时候数据会发送给 AI。

---

## 4. 最终 MVP 验收路径

最终只按这一条路径验收：

```text
dev login
-> 创建目标岗位
-> 粘贴 JD
-> 粘贴简历/项目经历
-> 同意临时上传生成摘要
-> 确认 CandidateProfile
-> 生成 JobBrief
-> 完成 5 题 Assessment
-> 查看 Assessment Report
-> 生成 1 天 TrainingPlan
-> 完成 1 个 TrainingTask
-> 开始 MockInterview
-> 回答并被追问
-> finish
-> 查看 MockInterview Report
-> 删除账号
```

只要这条路径不完整，就不算 MVP 完成。

## 5. 每次任务完成输出

每次实现后必须回复：

```text
改动文件：
- ...

如何运行：
- ...

如何测试：
- ...

已知限制：
- ...
```

禁止只回复“已完成”。禁止省略测试说明。
