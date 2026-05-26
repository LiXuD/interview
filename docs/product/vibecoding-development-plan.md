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
14. 删除账号，远端数据、本地登录态和远端同步缓存清理完成。

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
│   │   └── vibecoding-development-plan.md
│   ├── architecture/
│   │   └── code-wiki.md
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
- `DELETE /api/me` 删除远端用户数据后，iOS 必须清空 Keychain 和远端同步缓存。
- Post-MVP 本机 `CoachingMemoryArchive` 属于用户设备上的本地教练记忆归档，删除账号时默认保留；只有用户勾选“同时删除本机教练记忆文件”才删除。
- 后端必须使用 Spring Security 标准过滤器链解析 Bearer Token。
- 认证通过后，当前用户必须通过 `SecurityContextHolder` 获取。
- 禁止在业务接口里硬编码用户 ID 或绕过认证。

### 2.6 数据存储边界

纯本地 SwiftData：

- 简历原文。
- 用户未确认上传的项目经历原文。
- 本地草稿。
- 页面缓存状态。
- Post-MVP 本机 `CoachingMemoryArchive` 教练记忆归档。

远端 PostgreSQL：

- 用户确认后的 `CandidateProfile` 摘要。
- `InterviewTarget`。
- `JobBrief`。
- `AssessmentSession`。
- `TrainingPlan`。
- `MockInterview`。
- `Report`。
- Post-MVP 远端 `CoachingMemory`、纠错记录和训练观察摘要。
- 加密后的 OpenAI Provider API Key。

同步规则：

- App 启动后以远端业务数据为准同步。
- 简历原文永不落远端库。
- 用户删除账号后，远端数据删除，Keychain 和远端同步缓存同步清空。
- Post-MVP 本机教练记忆归档默认保留；重新登录或重新注册时不得自动上传，必须用户主动确认导入。

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

任务总览：

1. Walking Skeleton：iOS 启动、后端 health、iOS 调通后端。
2. OpenAPI 与基础 DTO：建立 `docs/api/openapi.yaml`，锁定 camelCase DTO。
3. Dev Login：Spring Security Bearer Token、`SecurityContextHolder`、Keychain。
4. Target CRUD：岗位目标完整闭环。
5. CandidateProfile 隐私链路：本地原文、临时上传、确认摘要、远端只存摘要。
6. Platform AI + JobBrief：第一个 AI 结构化输出完整跑通。
7. Assessment：5 题测评、评分、`Report(type=assessment)`。
8. TrainingPlan：基于短板生成 1 天任务。
9. MockInterview：文字面试、最近 6 轮上下文限制、`Report(type=mockInterview)`。
10. User OpenAI Provider：OpenAI-compatible Provider、加密 API Key、连接测试。
11. Delete Account：删除远端数据，清空远端同步缓存和 Keychain。
12. TestFlight Polish：空状态、加载状态、错误提示、隐私说明、首次使用引导。
13. Sign in with Apple：TestFlight 提审前置认证任务，不属于 Task1-12 MVP 功能闭环。
14. 平台默认真实 AI 接入：Post-MVP AI 质量任务，OpenAI-compatible 平台配置。
15. CandidateProfile AI 摘要：`draft-summary` 接入结构化 AI 输出。
16. AI Prompt 契约补齐：记录 task、输入边界、输出 DTO 和失败策略。
17. AI 质量迭代：优化 JobBrief、Assessment、Training、MockInterview 的 Prompt 和校验。
18. 开发环境真实 AI 基线：核心教练路径禁止静默走 stub。
19. 真实 AI 验收样例集：用典型岗位样例验证真实模型输出质量。
20. 固定 5 题结构化测评：题目包含维度、难度、意图和评分 rubric。
21. 教练记忆 Coaching Memory：沉淀训练、测评、模拟面试中的结构化用户理解。
22. 用户纠错与记忆可信度：支持用户纠正 AI 判断，并标注记忆来源和可信度。
23. 逐题评分与回答结构诊断：按题诊断回答内容、结构、追问风险和改进示范。
24. 自适应专项训练会话：根据回答动态决定追问、换角度、达标或停止。
25. 自适应模拟面试增强与本地记忆策略：增强真实面试追问，并明确本地记忆保留/删除规则。

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

目标：完成账号删除和本地登录态/业务缓存清理。

范围：

- `DELETE /api/me`
- 删除远端业务数据。
- iOS 清空远端同步缓存。
- iOS 清空 Keychain。

文件边界：

- `backend/src/main/java/com/interviewcoach/user`
- `backend/src/main/java/com/interviewcoach/auth`
- `ios/InterviewCoach/InterviewCoach/Core/Auth`
- `ios/InterviewCoach/InterviewCoach/Core/Storage`

验收：

- 删除账号后重新打开 App 无远端同步业务数据和登录态。
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

### Task 13: Sign in with Apple

目标：完成 TestFlight 提审前置认证任务。

说明：

- Task13 不属于 Task1-12 MVP 功能闭环。
- Task13 不改变 Task1-12 MVP 功能闭环验收结论。
- Task13 只解决 TestFlight 提审前的正式登录入口问题。

范围：

- `POST /api/auth/apple`。
- 后端真实校验 Apple `identityToken`。
- iOS Release 使用原生 Sign in with Apple 登录。
- iOS Debug 保留 dev login 入口。
- 生产/TestFlight 环境关闭 dev login。
- 更新 OpenAPI 与必要配置文档。

文件边界：

- `backend/src/main/java/com/interviewcoach/auth`
- `backend/src/main/java/com/interviewcoach/common/security`
- `backend/src/main/java/com/interviewcoach/common/api`
- `docs/api/openapi.yaml`
- `ios/InterviewCoach/InterviewCoach/Core/Auth`
- `ios/InterviewCoach/InterviewCoach/Features/Auth`
- iOS entitlement 与 project 配置。

验收：

- 有效 Apple 登录成功，后端返回现有 `LoginResponse`。
- 错误 token、issuer、audience、nonce 均登录失败。
- Release 构建不显示 dev login。
- Debug 构建保留 dev login。
- 后续业务接口继续使用 `Authorization: Bearer <token>`。
- 删除账号逻辑不变，仍清理远端数据、本地 SwiftData 和 Keychain。

限制：

- 不做 Apple authorization code exchange。
- 不保存 Apple refresh token。
- 不实现 OAuth revoke 生命周期。
- 不做 dev 账号与 Apple 账号自动合并。

---

## 4. Post-MVP AI Quality Roadmap

Task1-13 已完成 MVP 功能闭环和 TestFlight 提审前置认证任务。Post-MVP 阶段优先补齐 AI 质量闭环，不改变 MVP 已完成结论。

当前 AI 状态：

- 用户自定义 OpenAI-compatible Provider 已可用于业务 AI 调用。
- 平台默认 AI 已支持通过 `IC_PLATFORM_AI_*` 环境变量启用真实 OpenAI-compatible 后端模型；未启用时仍以 `LocalPlatformAiClient` 本地 stub 保持测试、离线演示和基础健康检查稳定。Task18 之后核心教练路径禁止静默走 stub。
- `POST /api/profiles/draft-summary` 已接入统一 AI 路由生成结构化 `CandidateProfileDraftDto`；后端仍自行计算 `rawTextLength`，并继续禁止保存或记录简历原文。
- iOS 仍禁止直接调用 AI，所有 AI 调用继续由后端统一代理。

### Task 14: 平台默认真实 AI 接入

目标：将平台默认 AI 从纯本地 stub 升级为可配置的 OpenAI-compatible 后端模型能力。

范围：

- 新增平台默认 AI 配置项。
- 复用现有 `OpenAiCompatibleClient`。
- 用户默认 Provider 仍优先于平台默认 AI。
- 未启用平台真实 AI 时，保留 `LocalPlatformAiClient` 作为测试、离线演示和基础健康检查兜底。
- 启用平台真实 AI 但配置缺失时，返回明确失败，不静默回退。

配置边界：

- `IC_PLATFORM_AI_ENABLED`
- `IC_PLATFORM_AI_BASE_URL`
- `IC_PLATFORM_AI_API_KEY`
- `IC_PLATFORM_AI_MODEL`
- `IC_PLATFORM_AI_MODE`

验收：

- 有用户默认 Provider 时优先调用用户 Provider。
- 无用户默认 Provider 且平台 AI 启用时调用平台配置。
- 平台 AI 未启用时继续使用本地 stub。
- 平台 AI 启用但配置缺失时失败明确。
- 平台密钥只通过环境变量或部署配置提供，不写入仓库、不返回 iOS、不写日志。

### Task 15: CandidateProfile AI 摘要

目标：将候选人摘要草稿从固定 stub 改为结构化 AI 输出。

范围：

- 为 `AiStructuredOutputService` 新增内部任务 `candidateProfileDraft`。
- `POST /api/profiles/draft-summary` 调用统一 AI 路由生成 `CandidateProfileDraftDto`。
- 后端继续自行计算 `rawTextLength`。
- iOS 请求和响应 DTO 保持不变。
- 用户仍必须先明确同意临时上传简历原文。

强约束：

- 简历原文只允许在内存中用于本次 AI 摘要生成。
- 简历原文不得落 PostgreSQL、Redis、文件、缓存或审计表。
- 禁止将 `resumeText`、`projectRawText`、`rawResume`、`originalText` 或任何原文片段写入日志。
- 后端仍必须返回强类型 DTO，禁止把 AI 原始字符串返回给 iOS。

验收：

- `draft-summary` 返回非固定占位文案的结构化摘要。
- `summary`、`skills`、`projects`、`experience` 字段可被用户编辑确认。
- AI 输出为空、字段缺失、非法 JSON 时返回统一 `AI_PARSE_FAILED`。
- 代码搜索确认没有原文日志输出。

### Task 16: AI Prompt 契约补齐

目标：补齐 AI 输入输出契约文档，降低后续 Prompt 迭代风险。

范围：

- 新增 `docs/ai/prompt-contracts.md`。
- 记录 AI task 名、输入边界、输出 DTO、解析失败策略。
- 首批覆盖 `candidateProfileDraft`。
- 同步列出现有 `jobBrief`、`assessmentQuestions`、`assessmentResult`、`trainingPlan`、`trainingFeedback`、`mockInterviewQuestion`、`mockInterviewReport`。

验收：

- 每个 task 都有输入来源、输出结构和失败策略说明。
- 文档明确 iOS 不解析 AI 原始文本。
- 文档明确 Anthropic Provider 不进入当前实现范围。

### Task 17: AI 质量迭代

目标：围绕 AI 面试教练定位提升输出质量和稳定性。

范围：

- 优化 JobBrief、Assessment、Training、MockInterview 的 Prompt。
- 补充典型 JD、简历、面试回答 fixture。
- 加强结构化输出校验和错误定位。
- 默认测试不依赖 live AI 调用。

验收：

- 典型样例下输出更贴合目标岗位和候选人摘要。
- 结构化输出失败时可定位到具体 task。
- 不引入非 MVP 产品方向，如题库社区、招聘投递、订阅付费、语音面试。

---

## 5. Post-MVP Real AI Adaptive Coaching Roadmap

Task1-17 已完成 MVP 功能闭环、TestFlight 提审前置认证和第一轮 Post-MVP AI 质量闭环。下一阶段以真实 AI 面试能力提升为核心，将产品从一次性测评工具升级为持续理解用户的 AI 面试教练。

阶段目标：

```text
真实 AI 基线
-> 固定 5 题结构化测评
-> 逐题评分与结构诊断
-> 教练记忆
-> 自适应专项训练
-> 自适应模拟面试
```

阶段硬约束：

- 开发环境也必须能连接真实 AI；核心教练路径不得静默使用 `LocalPlatformAiClient` stub。
- `LocalPlatformAiClient` 只能用于单元测试、CI 非 live AI 回归、明确标记的离线演示或基础健康检查。
- 初始能力测评保持固定 5 题一次性生成，保证稳定、公平、可比较。
- 专项训练和模拟面试必须自适应，根据用户上一轮回答决定追问、换角度、达标或停止。
- 教练记忆只保存结构化依据、用户回答摘要、评分、短板、纠错和训练观察，禁止保存 AI hidden chain-of-thought。
- 记忆必须区分 `confirmed`、`observed`、`corrected`、`inferred`、`rejected` 等来源和可信度。
- 远端 Coaching Memory 随删除账号删除；本机 `CoachingMemoryArchive` 默认保留，只有用户勾选“同时删除本机教练记忆文件”才删除。
- 重新登录或重新注册时，不得自动上传本机历史记忆，必须让用户主动确认导入。

### Task 18: 开发环境真实 AI 基线

目标：避免开发者被稳定 stub 输出误导，确保真实教练流程在开发环境也能暴露真实模型的质量波动、解析失败和幻觉风险。

状态：已完成。后端已提供 AI runtime status，iOS 核心教练入口已接入状态门禁，`stubOnly` 状态不再允许静默进入测评、训练和模拟面试核心流程。

范围：

- 后端提供当前 AI 运行状态：`realUserProvider`、`realPlatformProvider`、`stubOnly`、`unavailable`。
- iOS 在开始测评、训练和模拟面试前展示或检查当前 AI 状态。
- 测评出题、测评评分、训练反馈、专项训练、模拟面试追问和报告复盘等核心教练路径，在 `stubOnly` 状态下必须阻止继续。
- 本地开发文档明确真实 AI 配置要求。
- 保留单元测试、CI 非 live AI 回归、离线演示和基础健康检查使用 stub 的能力。

文件边界：

- `backend/src/main/java/com/interviewcoach/ai`
- `backend/src/main/resources/application.yml`
- `ios/InterviewCoach/InterviewCoach/Features/Settings`
- `docs/ai/provider-contracts.md`
- `README.md`

验收：

- 未配置用户 Provider 且平台真实 AI 未启用时，核心教练入口提示无法进入真实教练流程。
- 配置用户默认 Provider 时，核心教练路径使用用户 Provider。
- 配置平台真实 AI 时，核心教练路径使用平台 Provider。
- stub 状态不会被误展示为真实 AI。
- API Key、Authorization Header 和完整请求头不写入日志。

### Task 19: 真实 AI 验收样例集

目标：建立可重复运行的真实 AI 质量验收样例，验证模型是否真正贴合岗位、候选人经历和面试能力提升目标。

范围：

- 准备典型岗位样例：Java 后端/支付系统、AI 应用工程师/RAG Agent、数据平台/调度数仓。
- 每组样例包含目标岗位、JD、候选人摘要、样例回答。
- 覆盖 JobBrief、5 题测评、评分、训练计划、训练反馈、模拟面试追问。
- 默认 CI 不依赖 live AI；live AI 验收必须显式开启。

验收：

- 样例能验证输出是否贴合岗位和候选人摘要。
- 样例能发现 AI 是否虚构候选人经历。
- 样例能检查题目是否有区分度、评分是否具体、训练建议是否可执行。
- live AI 失败时能定位具体 AI task 和模型配置状态。

### Task 20: 固定 5 题结构化测评

目标：保持初始测评固定 5 题一次性生成，同时把题目从字符串升级为结构化测评对象。

范围：

- 测评题包含 `question`、`dimension`、`difficulty`、`intent`、`rubric`。
- iOS 仍优先展示题干，必要时可以展示维度和难度。
- 后端保存完整题目结构，评分时将 rubric 一并传给 AI。
- 建立稳定面试能力维度：`technicalDepth`、`projectSpecificity`、`systemThinking`、`tradeoffAwareness`、`failureHandling`、`communicationClarity`、`businessContext`。

验收：

- 每次初始测评仍为恰好 5 题。
- 题目结构字段完整，评分 rubric 不为空。
- 评分 Prompt 使用题目 rubric，而不是只使用题干。
- 输出不引入题库社区或刷题系统。

### Task 21: 教练记忆 Coaching Memory

目标：让产品逐步形成对用户的长期理解，避免每次训练都从零开始。

范围：

- 建立会话级记录：AI 问题、题目维度、难度、rubric、用户回答、评分、反馈、追问。
- 生成每日或训练计划级 `DailyCoachingMemory`，总结强项、短板、重复问题、已验证经历、未验证声明、下一步重点和避免重复内容。
- 建立长期 `LongTermCoachingProfile`，记录持续短板、进步趋势、已验证项目表达和下次追问方向。
- 后续 AI 使用记忆时只取必要摘要，禁止把完整历史无限塞入 Prompt。

强约束：

- 禁止保存 AI hidden chain-of-thought。
- 禁止保存简历原文。
- 禁止把 `inferred` 记忆当事实使用。
- Prompt 必须优先使用 `confirmed`、`observed`、`corrected`，对 `inferred` 只能通过追问验证。

验收：

- 完成测评、训练或模拟面试后能生成结构化教练记忆。
- 记忆记录来源和可信度。
- 后续训练可以使用历史短板和已验证经历定制问题。
- MockInterview 仍保持最多最近 6 轮上下文限制，不因记忆引入无限增长 Prompt。

### Task 22: 用户纠错与记忆可信度

目标：让用户能纠正 AI 的误判和幻觉，避免错误记忆污染后续训练。

范围：

- 用户可对报告、训练反馈、记忆摘要中的判断标记“不准确”。
- 用户可补充正确说法或否认 AI 推断。
- 记忆来源类型至少包括：`confirmed`、`observed`、`corrected`、`inferred`、`rejected`。
- 后续 Prompt 禁止把 `rejected` 内容再次当事实使用。

验收：

- 用户纠错后，后续 AI 不再重复被否认的经历或结论。
- `corrected` 内容优先级高于 `observed` 和 `inferred`。
- 纠错记录随远端账号数据删除；本机记忆归档按 Task 25 规则处理。

### Task 23: 逐题评分与回答结构诊断

目标：把测评报告从总分反馈升级为可训练的逐题诊断。

范围：

- 每道题单独评分并输出短板、亮点、改进示范和追问风险。
- 诊断回答结构：背景、任务、行动、结果、权衡、复盘。
- 聚合总分、能力维度分、主要短板和下一步训练建议。
- 输出必须继续解析为强类型 DTO，不允许 iOS 解析 AI 原始文本。

验收：

- 报告能指出每题具体问题，而不是泛泛建议。
- 改进示范不得虚构用户未确认的业务指标或项目结果。
- 逐题诊断能驱动专项训练任务生成。

### Task 24: 自适应专项训练会话

目标：将训练从“一题一答一反馈”升级为围绕短板的 2-4 轮自适应训练。

范围：

- 新增训练会话概念，围绕一个短板展开多轮追问。
- AI 根据用户上一轮回答返回下一步动作：`continue`、`pass`、`switch`、`stop`。
- `continue` 表示继续追问；`pass` 表示当前短板基本达标；`switch` 表示换相关角度；`stop` 表示用户明显卡住，需要先给讲解。
- 训练结束后生成本次训练总结和记忆更新。

验收：

- 自适应训练必须真实调用 AI。
- 每轮下一题必须基于用户上一轮回答。
- 训练轮数默认 2-4 轮，不扩展为多天课程系统。
- TrainingTask answer 仍不创建 Report，训练总结写入训练反馈或教练记忆。

### Task 25: 自适应模拟面试增强与本地记忆策略

目标：增强模拟面试的真实追问能力，并明确删除账号时本机教练记忆的保留策略。

范围：

- 模拟面试开场问题基于目标岗位、JD、候选人摘要、最近测评短板和可用教练记忆。
- 追问必须引用用户上一条回答中的具体内容。
- AI 可选择追深、换维度、要求举例、要求量化结果或要求解释权衡。
- finish 报告指出真实面试中最可能被继续追问的点。
- 删除账号时远端账号和远端业务数据必须删除；本机 `CoachingMemoryArchive` 默认保留。
- 删除账号页提供“同时删除本机教练记忆文件”选项，用户勾选后才删除本机记忆。
- 重新登录或重新注册时，检测到本机历史记忆也不得自动上传，必须用户主动确认导入。

验收：

- 模拟面试仍限制最多最近 6 轮上下文。
- 后续追问能体现历史短板和本轮回答细节。
- 删除账号后远端无法查询旧数据。
- 未勾选删除本机记忆时，本机教练记忆归档保留。
- 勾选删除本机记忆时，本机教练记忆归档被清除。
- 本机记忆归档不包含简历原文、API Key 或 AI hidden chain-of-thought。

---

## 6. 最终 MVP 验收路径

Task1-12 的最终验收仍按 MVP 功能闭环执行；Task13 是 TestFlight 提审前置认证任务，不改变 MVP 功能闭环验收路径。

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

## 7. 每次任务完成输出

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
