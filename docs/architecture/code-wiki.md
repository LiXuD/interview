# Interview Coach Code Wiki

生成日期：2026-05-25

本文档是项目代码级导航，用于帮助后续开发、Code Review 和问题定位。它描述当前仓库中的实际代码结构，不替代以下主约束与契约文档：

- `CLAUDE.md`：项目开发主约束。
- `docs/product/vibecoding-development-plan.md`：产品任务计划。
- `docs/api/openapi.yaml`：唯一 API 契约来源。
- `docs/privacy/data-policy.md`：隐私与数据策略。
- `docs/ai/provider-contracts.md`：AI Provider 规则。

## 1. 项目定位

本项目是 AI 技术岗面试教练 iOS App。核心闭环是：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

代码上采用 monorepo：

```text
interview/
├── backend/                    # Spring Boot 后端
├── ios/InterviewCoach/         # SwiftUI iOS App
├── docs/                       # API、产品、隐私、AI、架构文档
├── infra/docker-compose.yml    # 本地 PostgreSQL
├── AGENTS.md
├── CLAUDE.md
└── README.md
```

## 2. 运行视角

### 2.1 本地依赖

后端默认监听 `18080`，iOS 默认请求 `http://127.0.0.1:18080`。

```text
iOS Simulator
  -> URLSession
  -> http://127.0.0.1:18080
  -> Spring Security Bearer Token
  -> Spring MVC Controller
  -> Service
  -> Repository
  -> PostgreSQL
```

本地 PostgreSQL 配置在 `infra/docker-compose.yml`：

- database: `interview_coach`
- user: `ic_user`
- password: `ic_dev_password`
- port: `5432`

后端配置在 `backend/src/main/resources/application.yml`：

- `server.port: 18080`
- `spring.jpa.hibernate.ddl-auto: update`
- `app.auth.dev-login-enabled: true`
- `app.jwt.secret`
- `app.ai.encryption-key`
- `app.apple.services-id`

### 2.2 常用启动命令

项目约束要求 shell 命令使用 `rtk` 前缀。

```bash
rtk docker compose -f infra/docker-compose.yml up -d
cd backend
rtk mvn spring-boot:run
```

iOS 使用 Xcode 或 XcodeBuildMCP 运行：

```text
project: ios/InterviewCoach/InterviewCoach.xcodeproj
scheme: InterviewCoach
bundle id: com.interviewcoach.app
```

## 3. 后端总览

后端包名为 `com.interviewcoach`，采用模块化单体。

```text
backend/src/main/java/com/interviewcoach/
├── InterviewCoachApplication.java
├── common/
│   ├── api/        # DTO、HealthController
│   ├── error/      # 统一错误与异常
│   └── security/   # Spring Security、JWT、Apple token 验证
├── auth/           # 登录、Apple 登录、删除账号编排
├── user/           # 当前用户接口与 User 实体
├── target/         # 目标岗位
├── profile/        # 候选人摘要
├── ai/             # 平台 AI、自定义 Provider、结构化输出解析
├── jobbrief/       # 岗位画像
├── assessment/     # 5 题测评
├── training/       # 1 天训练计划与训练反馈
├── mockinterview/  # 文字模拟面试
└── report/         # 统一报告
```

### 3.1 后端分层约定

- Controller：只处理 HTTP 入参和返回 DTO。
- Service：承载业务规则、状态机、AI Prompt 组装、事务边界。
- Repository：Spring Data JPA 数据访问。
- Entity：只在后端内部使用，不直接返回给 iOS。
- DTO：集中在 `common/api`，使用 Java `record`，字段保持 camelCase。
- Error：集中在 `common/error`，由 `GlobalExceptionHandler` 转成统一 `ErrorResponse`。

## 4. 后端模块职责

### 4.1 common/api

`common/api` 是后端返回给 iOS 的强类型 DTO 层，也是 Controller 入参类型所在位置。

典型 DTO：

- Auth：`LoginRequest`、`LoginResponse`、`AppleLoginRequest`、`UserDto`
- Target：`InterviewTargetCreateRequest`、`InterviewTargetUpdateRequest`、`InterviewTargetDto`
- Profile：`CandidateProfileDraftRequest`、`CandidateProfileDraftDto`、`CandidateProfileConfirmRequest`、`CandidateProfileDto`
- JobBrief：`JobBriefGenerateRequest`、`JobBriefDto`、`SkillMapItem`
- Assessment：`AssessmentStartRequest`、`AssessmentAnswerRequest`、`AssessmentSessionDto`、`AssessmentResultDto`、`DimensionScore`
- Training：`TrainingPlanGenerateRequest`、`TrainingPlanDto`、`TrainingTaskDto`、`TrainingTaskAnswerRequest`、`TrainingFeedbackDto`
- MockInterview：`MockInterviewStartRequest`、`MockInterviewAnswerRequest`、`MockInterviewSessionDto`、`MockInterviewReportDto`
- Report：`ReportDto`
- AI Provider：`AiProviderCreateRequest`、`AiProviderDto`、`AiProviderTestRequest`、`AiProviderTestResponse`

### 4.2 common/security

关键文件：

- `SecurityConfig`：配置 stateless Spring Security；`/api/health`、`/api/auth/apple` 公开；`/api/auth/dev-login` 仅在 `app.auth.dev-login-enabled=true` 时公开；其他接口必须认证。
- `JwtAuthenticationFilter`：解析 Bearer Token 并写入 `SecurityContextHolder`。
- `JwtTokenProvider`：签发和校验 JWT。
- `SecurityUtils`：业务 Controller 从这里获取当前用户，避免硬编码用户 ID。
- `AppleTokenVerifier`：拉取 Apple JWKS，校验 identityToken 的 issuer、audience、签名、nonce。

认证边界：

```text
HTTP Authorization: Bearer <token>
  -> JwtAuthenticationFilter
  -> JwtTokenProvider
  -> UserRepository
  -> SecurityContextHolder principal = User
  -> Controller 使用 SecurityUtils.currentUser()
```

### 4.3 auth/user

`AuthController` 提供：

- `POST /api/auth/dev-login`
- `POST /api/auth/apple`
- `POST /api/auth/logout`

`UserController` 提供：

- `GET /api/me`
- `DELETE /api/me`

`AuthService` 负责：

- Dev Login：按 username 查找或创建用户，并签发 JWT。
- Apple Login：校验 `identityToken` 和必填 `nonce`，通过 Apple sub 查找或创建用户，并签发 JWT。
- Delete Account：按依赖顺序删除用户相关数据，包括测评、报告、模拟面试、训练、AI Provider、岗位画像、候选人摘要、目标岗位和用户。

### 4.4 target

目标岗位是 MVP 闭环入口。

主要文件：

- `InterviewTarget`：目标岗位实体，包含 title、jd、status、createdAt、updatedAt。
- `InterviewTargetController`：提供创建、列表、详情、更新、删除。
- `InterviewTargetService`：做用户隔离、状态白名单、删除级联。
- `InterviewTargetRepository`：按 `id + userId` 查询，防止跨用户访问。

### 4.5 profile

候选人摘要模块承接简历隐私链路。

主要文件：

- `CandidateProfile`：只保存确认后的摘要、技能、项目、经历。
- `CandidateProfileController`：
  - `POST /api/profiles/draft-summary`
  - `POST /api/profiles/confirm`
  - `GET /api/profiles/current?targetId=...`
- `CandidateProfileService`：
  - draft 阶段只接收原文生成摘要 DTO。
  - confirm 阶段保存用户确认后的结构化摘要。

隐私边界：

- 简历原文不落 PostgreSQL。
- 后端只在 draft 请求内存中使用原文。
- iOS 本地可以保留用户输入，用于编辑体验。

### 4.6 ai

AI 模块是后端唯一允许接触模型原始响应的地方。

主要文件：

- `PlatformAiClient`：平台默认 AI 抽象。
- `LocalPlatformAiClient`：本地 stub 实现，返回结构化 JSON。
- `OpenAiCompatibleClient`：支持 OpenAI-compatible Provider，包含 chatCompletions 与 responses 模式。
- `AiProvider`：用户自定义 Provider 实体，API Key 加密保存。
- `ApiKeyEncryption`：AES-GCM 加解密 API Key。
- `AiProviderService`：Provider CRUD、默认 Provider、跨用户隔离、连接测试。
- `AiStructuredOutputService`：统一调用 Provider、解析 JSON、校验强类型 DTO。

AI 输出规则：

```text
Service 组装 AiPrompt
  -> AiStructuredOutputService.generateXxx()
  -> 选择用户默认 Provider 或平台 AI
  -> 获取 raw JSON
  -> ObjectMapper 解析为强类型 DTO
  -> validateXxx()
  -> 解析失败最多重试 1 次
  -> 仍失败抛 AiParseException
```

iOS 不解析 AI 原始字符串，后端也不把 AI 原始字符串作为业务响应返回给 iOS。

### 4.7 jobbrief

岗位画像模块基于目标岗位和候选人摘要生成 `JobBriefDto`。

主要文件：

- `JobBrief`：岗位画像实体。
- `JobBriefSkill`：技能地图 embeddable。
- `JobBriefController`：
  - `POST /api/job-briefs/generate`
  - `GET /api/job-briefs/{targetId}`
- `JobBriefService`：校验 target/profile 归属，组装 Prompt，持久化岗位画像。

输出结构：

- roleSummary
- skillMap
- mustHaveSkills
- niceToHaveSkills
- businessContext
- interviewTopics
- candidateMatch
- riskAreas
- confidence

### 4.8 assessment

测评模块负责 5 题测评和 assessment report。

主要文件：

- `AssessmentSession`：保存问题列表、回答列表、状态、当前题号。
- `AssessmentResult`：保存总分、维度分、优势、短板、下一步。
- `AssessmentController`：
  - `POST /api/assessments/start`
  - `POST /api/assessments/{id}/answers`
  - `POST /api/assessments/{id}/finish`
  - `GET /api/assessments/{id}`
- `AssessmentService`：生成 5 题、收集答案、完成评分、创建 `Report(type=assessment)`。

状态流：

```text
startAssessment
  -> generateAssessmentQuestions(exactly 5)
  -> AssessmentSession(status=in_progress, questionIndex=0)

submitAnswer x5
  -> answers.add(answer)
  -> questionIndex += 1

finishAssessment
  -> require answers.size == totalQuestions
  -> generateAssessmentResult
  -> AssessmentResult
  -> AssessmentSession(status=completed)
  -> Report(type=assessment, content=json)
```

### 4.9 training

训练模块基于测评短板生成 1 天训练任务，并为单个任务生成反馈。

主要文件：

- `TrainingPlan`：训练计划实体。
- `TrainingTask`：训练任务实体。
- `TrainingFeedback`：训练反馈实体。
- `TrainingPlanController`：
  - `POST /api/training-plans/generate`
  - `GET /api/training-plans/{targetId}`
- `TrainingTaskController`：
  - `POST /api/training-tasks/{id}/answer`
  - `PATCH /api/training-tasks/{id}/complete`
- `TrainingService`：生成任务、答题反馈、标记完成。

生命周期：

```text
generateTrainingPlan
  -> 基于最新 AssessmentResult
  -> 生成 2-4 个训练任务
  -> TrainingPlan + TrainingTask[]

answerTrainingTask
  -> generateTrainingFeedback
  -> TrainingFeedback
  -> 不创建 Report

completeTrainingTask
  -> TrainingTask(status=completed)
```

### 4.10 mockinterview

模拟面试模块负责文字问答和 mock interview report。

主要文件：

- `MockInterview`：会话实体，包含 status、targetId、user、messages。
- `MockInterviewMessage`：消息实体，role 为 `assistant` 或 `user`。
- `MockInterviewController`：
  - `POST /api/mock-interviews/start`
  - `POST /api/mock-interviews/{id}/answer`
  - `POST /api/mock-interviews/{id}/finish`
  - `GET /api/mock-interviews/{id}`
- `MockInterviewService`：生成首问、追问、结束报告。

Prompt 上下文限制：

```text
MAX_CONTEXT_TURNS = 6
MAX_CONTEXT_MESSAGES = 12
```

`submitAnswer` 和 `finishInterview` 都只把最近 12 条 message 放进 Prompt，避免 Prompt 随对话轮次无限增长。

状态流：

```text
startInterview
  -> generate first assistant question
  -> MockInterview(status=in_progress)

submitAnswer
  -> append user answer
  -> use recent context
  -> generate next assistant question

finishInterview
  -> use recent context
  -> generate MockInterviewReportDto
  -> MockInterview(status=completed)
  -> Report(type=mockInterview, content=json)
```

### 4.11 report

统一报告模块承载测评报告和模拟面试报告。

主要文件：

- `Report`：包含 `userId`、`targetId`、`type`、`content`、`createdAt`。
- `ReportController`：
  - `GET /api/reports?targetId=...`
  - `GET /api/reports/{id}`
- `ReportService`：按当前用户和 targetId/reportId 查询，防止跨用户访问。

报告类型：

- `assessment`
- `mockInterview`

注意：`Report.content` 存储的是后端已解析并重新序列化后的 DTO JSON，不是模型原始文本。

## 5. API 地图

当前 OpenAPI 包含 29 个路径：

| 模块 | 路径 |
| --- | --- |
| Health | `GET /api/health` |
| Auth | `POST /api/auth/dev-login` |
| Auth | `POST /api/auth/apple` |
| Auth | `POST /api/auth/logout` |
| User | `GET /api/me` |
| User | `DELETE /api/me` |
| Target | `POST /api/targets` |
| Target | `GET /api/targets` |
| Target | `GET /api/targets/{id}` |
| Target | `PATCH /api/targets/{id}` |
| Target | `DELETE /api/targets/{id}` |
| Profile | `POST /api/profiles/draft-summary` |
| Profile | `POST /api/profiles/confirm` |
| Profile | `GET /api/profiles/current?targetId=...` |
| JobBrief | `POST /api/job-briefs/generate` |
| JobBrief | `GET /api/job-briefs/{targetId}` |
| Assessment | `POST /api/assessments/start` |
| Assessment | `POST /api/assessments/{id}/answers` |
| Assessment | `POST /api/assessments/{id}/finish` |
| Assessment | `GET /api/assessments/{id}` |
| Training | `POST /api/training-plans/generate` |
| Training | `GET /api/training-plans/{targetId}` |
| Training | `POST /api/training-tasks/{id}/answer` |
| Training | `PATCH /api/training-tasks/{id}/complete` |
| MockInterview | `POST /api/mock-interviews/start` |
| MockInterview | `POST /api/mock-interviews/{id}/answer` |
| MockInterview | `POST /api/mock-interviews/{id}/finish` |
| MockInterview | `GET /api/mock-interviews/{id}` |
| Report | `GET /api/reports?targetId=...` |
| Report | `GET /api/reports/{id}` |
| AI Provider | `POST /api/ai-providers` |
| AI Provider | `GET /api/ai-providers` |
| AI Provider | `POST /api/ai-providers/test` |
| AI Provider | `PATCH /api/ai-providers/{id}/default` |
| AI Provider | `DELETE /api/ai-providers/{id}` |

说明：OpenAPI 中 `/api/targets`、`/api/targets/{id}`、`/api/ai-providers` 等路径下包含多个 HTTP method，因此路径数量和端点数量不同。

## 6. iOS 总览

iOS 代码位于：

```text
ios/InterviewCoach/InterviewCoach/
├── App/
├── Core/
│   ├── API/
│   ├── Auth/
│   ├── Security/
│   ├── Storage/LocalModels/
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
│   ├── Settings/
│   └── Onboarding/
└── Resources/
```

### 6.1 App

主要文件：

- `InterviewCoachApp.swift`：App 入口，注入 SwiftData model container。
- `AppRootView.swift`：认证态分流、后端健康状态、设置入口、onboarding cover。

根状态：

```text
AuthService.isAuthenticated == false
  -> LoginView

AuthService.isAuthenticated == true
  -> TargetListView
  -> optional OnboardingView
  -> SettingsView sheet
```

### 6.2 Core/API

`APIClient` 是全局网络入口：

- `actor APIClient`
- baseURL: `http://127.0.0.1:18080`
- 自动设置 `Content-Type: application/json`
- 默认附带 Keychain 中的 Bearer Token
- 401 映射为 `APIError.unauthorized`
- 非 2xx 尝试解析 `ErrorResponseDTO`

DTO 文件和后端 DTO 对应：

- `AuthDTO.swift`
- `InterviewTargetDTO.swift`
- `CandidateProfileDTO.swift`
- `JobBriefDTO.swift`
- `AssessmentDTO.swift`
- `TrainingDTO.swift`
- `MockInterviewDTO.swift`
- `ReportDTO.swift`
- `AiProviderDTO.swift`
- `HealthResponseDTO.swift`
- `ErrorResponseDTO.swift`

### 6.3 Core/Auth 与 Core/Security

`AuthService` 负责：

- devLogin
- appleLogin
- logout
- deleteAccount
- fetchCurrentUser
- 保存/删除 Keychain token
- 删除账号后清理本地 SwiftData 数据

`KeychainHelper` 负责 token 的 Keychain 保存、读取、删除。

### 6.4 Core/Storage

SwiftData 本地模型：

- `TargetLocal`
- `CandidateProfileLocal`

用途：

- 网络失败时提供局部 fallback。
- 删除账号或 logout 时清理本地数据。
- 网络 DTO 与 SwiftData Model 分离，避免混用。

### 6.5 Core/UI

共享 UI：

- `DateHelper`
- `ErrorBanner`
- `LoadingOverlay`
- `TargetStatusHelper`

## 7. iOS Feature 地图

### 7.1 Auth

文件：

- `LoginView.swift`
- `DevLoginView.swift`

能力：

- Release/Debug 都展示 Sign in with Apple。
- Debug 下显示开发模式登录入口。
- Sign in with Apple 会生成 raw nonce，把 SHA-256 nonce 交给 Apple request，并把 raw nonce 发给后端校验。

### 7.2 Onboarding

文件：

- `OnboardingView.swift`

能力：

- 登录后的首次引导。
- 使用 `@AppStorage("hasCompletedOnboarding")` 标记完成状态。

### 7.3 Targets

文件：

- `TargetListView.swift`
- `TargetCreateView.swift`
- `TargetDetailView.swift`

能力：

- 列表、创建、详情、更新、删除目标岗位。
- 目标详情作为 MVP 功能入口，跳转到简历、岗位画像、测评、训练、模拟面试、报告。
- Debug 支持 `-ICDebugPrefillTarget` 启动参数预填目标岗位。

### 7.4 Profiles

文件：

- `ProfileInputView.swift`
- `ProfileConfirmView.swift`

能力：

- 展示隐私说明。
- 用户输入简历/项目经历。
- 弹窗确认“临时上传原文生成摘要”。
- 生成 draft 后进入摘要确认页。
- 保存确认后的结构化摘要。

### 7.5 JobBrief

文件：

- `JobBriefView.swift`

能力：

- 生成或重新生成岗位画像。
- 展示岗位概览、置信度、技能地图、必备技能、加分技能、业务上下文、面试主题、匹配点、风险点。

### 7.6 Assessment

文件：

- `AssessmentView.swift`
- `AssessmentResultView.swift`

能力：

- 开始 5 题测评。
- 逐题答题。
- 全部完成后触发评分。
- 展示总分、维度分、优势、短板、下一步。

### 7.7 Training

文件：

- `TrainingPlanView.swift`
- `TrainingTaskView.swift`

能力：

- 生成 1 天训练计划。
- 展示训练任务列表。
- 单个任务答题。
- 展示训练反馈、优化答案、追问、建议复习点。
- 标记任务完成。

### 7.8 MockInterview

文件：

- `MockInterviewView.swift`
- `MockInterviewResultView.swift`

能力：

- 开始文字模拟面试。
- 展示当前面试官问题。
- 提交回答并获得下一问。
- 结束面试并生成面试报告。
- 展示总分、维度分、总结、优势、短板、改进答案、下一步训练。

### 7.9 Reports

文件：

- `ReportListView.swift`
- `ReportDetailView.swift`

能力：

- 按 targetId 加载报告列表。
- 按 `type` 分组展示测评报告和面试报告。
- `ReportListView` 会解析 `ReportDTO.content` 中的 DTO JSON，用于展示分数和摘要。
- `ReportDetailView` 展示单个报告详情。

### 7.10 Settings

文件：

- `SettingsView.swift`
- `AiProviderListView.swift`
- `AiProviderCreateView.swift`
- `PrivacyPolicyView.swift`

能力：

- 管理 OpenAI-compatible 自定义 Provider。
- 创建 Provider 前可测试连接。
- 切换默认 Provider。
- 删除 Provider。
- 查看隐私政策。
- 删除账号。

## 8. 核心业务流程

### 8.1 登录流程

```text
Debug dev login:
DevLoginView
  -> AuthService.devLogin()
  -> POST /api/auth/dev-login
  -> AuthService(dev backend)
  -> UserRepository find/create
  -> JwtTokenProvider.generateToken
  -> iOS KeychainHelper.saveToken
  -> TargetListView

Apple login:
LoginView
  -> generate rawNonce
  -> ASAuthorizationAppleIDRequest.nonce = sha256(rawNonce)
  -> AuthService.appleLogin(identityToken, fullName, rawNonce)
  -> POST /api/auth/apple
  -> AppleTokenVerifier.verifyAndGetSub(identityToken, rawNonce)
  -> UserRepository find/create by appleUserId
  -> JwtTokenProvider.generateToken
```

### 8.2 MVP 主流程

```text
TargetCreateView
  -> POST /api/targets

ProfileInputView
  -> POST /api/profiles/draft-summary
  -> ProfileConfirmView
  -> POST /api/profiles/confirm

JobBriefView
  -> POST /api/job-briefs/generate

AssessmentView
  -> POST /api/assessments/start
  -> POST /api/assessments/{id}/answers x5
  -> POST /api/assessments/{id}/finish
  -> AssessmentResultView
  -> Report(type=assessment)

TrainingPlanView
  -> POST /api/training-plans/generate
  -> TrainingTaskView
  -> POST /api/training-tasks/{id}/answer
  -> PATCH /api/training-tasks/{id}/complete

MockInterviewView
  -> POST /api/mock-interviews/start
  -> POST /api/mock-interviews/{id}/answer
  -> POST /api/mock-interviews/{id}/finish
  -> MockInterviewResultView
  -> Report(type=mockInterview)

ReportListView
  -> GET /api/reports?targetId=...
```

### 8.3 删除账号流程

```text
SettingsView
  -> AuthService.deleteAccount()
  -> DELETE /api/me
  -> AuthService.deleteUser(userId)
  -> delete assessment result/session
  -> delete reports
  -> delete mock interviews
  -> delete training plans
  -> delete AI providers
  -> delete job briefs
  -> delete candidate profiles
  -> delete targets
  -> delete user
  -> iOS logout()
  -> Keychain token deleted
  -> SwiftData local target/profile deleted
  -> LoginView
```

## 9. 数据模型关系

概念关系：

```text
User
  ├── InterviewTarget
  │     ├── CandidateProfile
  │     ├── JobBrief
  │     ├── AssessmentSession
  │     │     └── AssessmentResult
  │     ├── TrainingPlan
  │     │     └── TrainingTask
  │     │           └── TrainingFeedback
  │     ├── MockInterview
  │     │     └── MockInterviewMessage
  │     └── Report
  └── AiProvider
```

关键生命周期约束：

- `AssessmentSession.finish` 创建 `Report(type=assessment)`。
- `MockInterview.finish` 创建 `Report(type=mockInterview)`。
- `TrainingTask.answer` 只创建或返回 `TrainingFeedback`，不创建 `Report`。
- 简历原文不建实体，不入库。
- API Key 只保存加密值，不返回给 iOS。

## 10. 安全与隔离

### 10.1 用户隔离

后端业务查询必须使用当前用户：

```text
Controller
  -> SecurityUtils.currentUser()
  -> Service(user or userId)
  -> Repository findBy...AndUserId(...)
```

重点 Repository 方法通常包含：

- `findByIdAndUserId`
- `findByTargetIdAndUserId`
- `deleteByUserId`

### 10.2 认证策略

公开接口：

- `GET /api/health`
- `POST /api/auth/apple`
- `POST /api/auth/dev-login`，仅 dev-login enabled 时公开。

其他接口均需要 Bearer Token。

### 10.3 隐私策略

简历：

- iOS 本地输入。
- draft summary 时临时发送到后端。
- 后端不落库原文。
- 确认后只保存摘要、技能、项目、经历。

API Key：

- iOS 创建 Provider 时发送 API Key。
- 后端用 AES-GCM 加密保存。
- 后端返回 `AiProviderDto` 不包含 API Key。

Apple 登录：

- 后端校验 Apple identityToken。
- `nonce` 是必填字段。
- 后端 hash raw nonce 后对比 JWT nonce claim。

## 11. 测试地图

后端测试位于：

```text
backend/src/test/java/com/interviewcoach/
```

主要覆盖：

- Auth：dev login、当前用户、删除账号、Apple 登录错误响应。
- Target：CRUD、状态校验、用户隔离。
- Profile：摘要 draft/confirm、隐私链路。
- JobBrief：生成、查询、缺失依赖。
- Assessment：start/answer/finish、报告创建。
- Training：计划生成、任务答题、完成。
- MockInterview：start/answer/finish、报告创建、上下文限制。
- AI Provider：CRUD、默认 Provider、连接测试、跨用户隔离、API Key 不泄露。
- Health：后端可用性。

常用命令：

```bash
cd backend
rtk mvn -q test
```

iOS 当前主要通过 Xcode/XcodeBuildMCP 编译和模拟器手动流程验证。

## 12. 常见定位入口

### 12.1 某个接口返回 401

检查：

1. iOS `KeychainHelper.loadToken()` 是否有 token。
2. `APIClient` 调用是否使用默认 `authorized: true`。
3. 后端 `JwtAuthenticationFilter` 是否解析 Bearer Token。
4. 业务 Controller 是否通过 `SecurityUtils.currentUser()` 获取用户。

### 12.2 AI 返回解析失败

检查：

1. `AiStructuredOutputService.generateAndValidate()` 抛出的是否是 `AiParseException`。
2. 对应 `validateXxx()` 是否要求字段或枚举更严格。
3. `LocalPlatformAiClient` 或自定义 Provider 返回 JSON 是否匹配 DTO。
4. OpenAPI 与 iOS DTO 是否同步。

### 12.3 数据串用户

检查：

1. Repository 查询是否包含 `userId`。
2. Service 是否传入当前用户。
3. Controller 是否从 `SecurityUtils.currentUser()` 获取用户。
4. 是否误用纯 `findById()` 查询业务归属对象。

### 12.4 报告展示异常

检查：

1. 后端 `Report.content` 是否是对应 DTO 序列化结果。
2. `Report.type` 是否是 `assessment` 或 `mockInterview`。
3. iOS `ParsedReportContent.parse(from:)` 是否能解码。
4. `ReportDetailView` 是否覆盖该类型。

### 12.5 模拟面试 Prompt 过长

检查：

1. `MockInterviewService.MAX_CONTEXT_TURNS` 是否仍为 6。
2. `MAX_CONTEXT_MESSAGES` 是否仍为 12。
3. `getContextMessages()` 是否仍只取最近 message。
4. `buildFinishPrompt()` 是否未传完整历史。

## 13. 当前已知实现特征

- 本地默认 AI 通过 `LocalPlatformAiClient` 返回稳定 stub JSON，便于完整流程演示。
- 自定义 Provider 支持 OpenAI-compatible `chatCompletions` 与 `responses` 两种模式。
- iOS Debug 下保留 dev login；Release 下不显示开发登录入口。
- `TargetCreateView` Debug 支持启动参数预填目标岗位。
- 报告详情和列表会解析报告 JSON 做结构化展示。
- `README.md` 中部分 Task13 描述可能与后续修复后的实现细节存在文字滞后；代码事实以当前源码和 OpenAPI 为准。

## 14. 修改代码时的快速检查清单

后端：

- Controller 是否返回 DTO，不返回 Entity。
- Service 是否从当前用户维度做权限隔离。
- 新 API 是否同步 `docs/api/openapi.yaml`。
- JSON 字段是否是 camelCase。
- AI 原始输出是否只停留在 `ai` 模块内部。
- 简历原文/API Key 是否避免日志和明文持久化。
- MockInterview Prompt 是否保持最多最近 12 条 message。

iOS：

- DTO 是否与 OpenAPI/后端 DTO 对齐。
- 网络调用是否通过 `APIClient`。
- token 是否只通过 `KeychainHelper` 管理。
- SwiftData local model 是否不与网络 DTO 混用。
- Debug-only 入口是否包在 `#if DEBUG`。
- 删除账号/退出登录是否清理本地状态。

文档：

- API 改动更新 `docs/api/openapi.yaml`。
- 隐私或数据生命周期改动更新 `docs/privacy/data-policy.md`。
- AI Provider 或 Prompt 契约改动更新 `docs/ai/`。
- 产品范围改动更新 `docs/product/vibecoding-development-plan.md`。

