# AI 面试教练 App 开发约束

本文档是本项目 vibecoding 开发的主约束文件。任何 AI 开发代理在改动本项目之前，必须先阅读并遵守本文档。若用户的最新明确指令与本文档冲突，以用户最新明确指令为准；否则以本文档为准。

## 1. 项目定位

本项目是一款面向技术岗求职者的 iOS App：用户输入目标岗位、JD、简历或项目经历后，App 通过 AI 完成岗位研究、能力测评、专项训练、模拟面试和复盘报告。

本项目不是题库 App，不是刷题社区，不是简历润色工具，不是招聘投递工具。所有功能必须服务“AI 面试教练”这个定位。

核心闭环必须保持为：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

MVP 只验收这条最窄闭环。只要这条路径不完整，就不算 MVP 完成。

Task 1-33 全部完成：MVP 功能闭环、Post-MVP AI 质量闭环、Post-MVP Real AI Adaptive Coaching、Phase 3 持续训练伙伴与 AI 质量运营闭环。Spring AI 底座迁移 Phase 2-6 已完成。MVP 完成后的新增开发必须继续服务 AI 面试教练定位，并且只能按 `docs/product/vibecoding-development-plan.md` 中已批准的 Post-MVP 任务推进。

## 2. MVP 范围

MVP 必须支持：

- 用户登录。
- 创建目标岗位。
- 粘贴 JD。
- 粘贴简历或项目经历。
- 明确同意临时上传原文生成摘要。
- 确认服务端返回的 `CandidateProfile` 摘要。
- 生成 `JobBrief` 岗位画像。
- 完成 5 题 `Assessment`。
- 生成 1 天 `TrainingPlan`。
- 完成 1 次文字 `MockInterview`。
- 查看统一 `Report`。
- 删除账号，并清理远端数据、本地登录态和远端同步缓存。

MVP 禁止实现：

- 题库社区。
- 招聘投递。
- 企业端面试系统。
- 订阅付费。
- 多人协作。
- 语音面试。
- 简历自动投递。
- 任意未进入已批准计划的模型厂商适配。
- Anthropic 自定义 Provider。
- 未进入 Phase 3 受控范围的多天复杂课程系统或刷题计划。

## 3. 开发方式

本项目采用 vibecoding 方式开发。每次任务必须小而清晰。

每次实现必须遵守：

- 必须只实现当前任务指定的一个小模块。
- 禁止一次实现多个 Phase。
- 禁止顺手增加计划外页面、计划外 API、计划外数据表。
- 禁止为了“看起来完整”加入不在当前批准任务范围内的功能。
- 必须优先保证当前垂直切片可运行、可测试、可演示。
- 必须保持已有功能可运行。
- 修改完成后必须说明：改了哪些文件、如何运行、如何测试、已知限制。

每次任务完成前必须检查：

- App 是否能编译或至少当前改动不破坏编译结构。
- 后端是否能启动或至少当前改动不破坏启动结构。
- API 是否符合 OpenAPI 文档。
- JSON 字段是否统一 camelCase。
- 后端是否返回强类型 DTO。
- 是否引入计划外功能。
- 是否破坏隐私约束。
- 是否把 AI 原始字符串直接返回给 iOS。
- 是否把完整模拟面试历史塞进模型上下文。

## 4. 技术栈

iOS 必须使用：

- SwiftUI。
- iOS 17+。
- SwiftData。
- Keychain。
- async/await。
- URLSession。
- Codable DTO。
- MVVM 或轻量 feature-based 架构。

后端必须使用：

- Spring Boot 3。
- PostgreSQL。
- Redis。
- Spring Security。
- OpenAPI 文档。
- 模块化单体。

AI 调用必须遵守：

- 后端统一代理 AI 请求。
- iOS 禁止直接调用大模型。
- 后端默认提供平台 AI。
- MVP 支持 OpenAI-compatible 自定义 Provider。
- Anthropic 协议只作为 post-MVP 扩展点预留，MVP 禁止实现。
- Post-MVP 平台默认真实 AI 只能采用 OpenAI-compatible 后端代理配置。
- Post-MVP Real AI Adaptive Coaching 阶段，开发环境也必须能连接真实 AI；测评、训练、模拟面试等核心教练路径禁止静默使用 stub。
- Phase 3 持续训练伙伴阶段必须先补齐 AI 可观测与真实 AI 回归评测，再扩展多天训练、进步追踪和多轮模拟面试。
- 产品能力验收、阶段评估、面向客户可用性判断、AI 质量审查必须显式使用真实 AI（默认读取 `backend/.env` 中的 `IC_LIVE_AI_TEST` 与 `IC_PLATFORM_AI_*` 配置），至少运行相关 live AI smoke；重要 AI 行为变更必须运行完整 `AiContentQualityTest` 或说明未运行原因。
- stub、mock、`LocalPlatformAiClient` 和默认 CI 测试只能证明工程结构、DTO、权限、持久化和解析逻辑未坏，禁止作为 AI 产品体验、AI 输出质量或“可面向客户”的验收依据。
- 平台 API Key 只能通过环境变量或部署配置提供，禁止写入仓库、返回给 iOS 或写入日志。

## 5. 项目目录与模块边界规范

项目根目录必须采用 monorepo 结构。禁止在项目根目录随意创建业务代码文件。

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
│       ├── provider-contracts.md
│       └── spring-ai-long-term-foundation-plan.md
├── ios/
│   └── InterviewCoach/
├── backend/
│   ├── build.gradle 或 pom.xml
│   └── src/
├── web/
│   └── admin/          # React 管理端（Token 用量看板）
├── infra/
│   └── docker-compose.yml
└── scripts/
```

### 5.1 iOS 目录规范

`ios/InterviewCoach/` 下必须采用 feature-based 结构：

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
│   ├── Settings/
│   └── Onboarding/
└── Resources/
```

iOS 目录规则：

- 网络 DTO 必须放在 `ios/InterviewCoach/InterviewCoach/Core/API/DTO`。
- 网络 DTO 必须对应后端 camelCase JSON。
- SwiftData 本地模型必须放在 `ios/InterviewCoach/InterviewCoach/Core/Storage/LocalModels`。
- 网络 DTO 和 SwiftData Model 禁止混用。
- 每个 Feature 内部可以按需创建 `Views`、`ViewModels`、`Models`。
- iOS 禁止直接调用大模型，只能调用后端 API。
- iOS 共享 UI 组件必须放在 `Core/UI`，禁止散落在业务 Feature 之外的临时目录。

### 5.2 后端目录规范

后端包名必须统一使用：

```text
com.interviewcoach
```

Spring Boot 目录必须遵守：

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

后端目录规则：

- Controller 只接收 Request DTO、返回 Response DTO。
- Service 承载业务逻辑。
- Repository 只负责数据访问。
- Entity 禁止直接返回给 iOS。
- AI 原始响应只能停留在 `ai` 模块内部。
- 业务模块只能拿到解析后的强类型 DTO。
- Spring Security 配置必须放在 `common/security` 或 `auth` 下。
- 禁止把 Spring Security 配置散落在业务模块里。
- 通用错误响应、分页、requestId 等 API 基础设施必须放在 `common/api` 或 `common/error`。

### 5.3 文档目录规范

文档目录职责必须固定：

- `docs/api/openapi.yaml`：唯一 API 契约来源。
- `docs/product/vibecoding-development-plan.md`：产品范围、MVP 路径、Task 计划和 Post-MVP 路线。
- `docs/architecture/code-wiki.md`：系统架构、数据流、模块边界和代码导航。
- `docs/privacy/data-policy.md`：简历、API Key、账号删除、日志脱敏策略。
- `docs/ai/prompt-contracts.md`：AI 输入输出 JSON、Prompt 版本、解析失败策略。
- `docs/ai/provider-contracts.md`：平台 AI、OpenAI-compatible Provider 规则。
- `docs/ai/spring-ai-long-term-foundation-plan.md`：Spring AI 底座迁移方案、阶段进度与设计决策。

禁止把正式产品、架构、隐私、AI 契约文档散落到项目根目录。临时调研笔记必须放在 `docs/research/`，并且不得包含 API Key、简历原文或隐私数据。

### 5.4 基础设施与脚本目录规范

- `infra/` 只放本地开发和部署相关配置，例如 `docker-compose.yml`。
- `scripts/` 只放可重复运行的开发脚本，例如启动、检查、生成 OpenAPI 客户端。
- 脚本必须可重复执行，禁止依赖本机私有绝对路径，除非脚本名称或注释明确说明只用于本机临时调试。
- 本机临时脚本必须以 `.local` 命名，并且不得提交敏感信息。

### 5.5 Vibecoding 文件边界

执行每个任务时，必须限制改动范围：

- Walking Skeleton：只允许改 `ios/`、`backend/` 的 health 相关入口和 `docs/api/openapi.yaml`。
- OpenAPI 与基础 DTO：主要改 `docs/api/openapi.yaml`、iOS DTO、后端 DTO。
- Dev Login：主要改 `backend/src/main/java/com/interviewcoach/auth`、`backend/src/main/java/com/interviewcoach/common/security`、`ios/InterviewCoach/InterviewCoach/Core/Auth`。
- Target CRUD：主要改 `backend/src/main/java/com/interviewcoach/target`、`ios/InterviewCoach/InterviewCoach/Features/Targets`。
- CandidateProfile：主要改 `backend/src/main/java/com/interviewcoach/profile`、`ios/InterviewCoach/InterviewCoach/Features/Profiles`、`docs/privacy`。
- JobBrief：主要改 `backend/src/main/java/com/interviewcoach/jobbrief`、`backend/src/main/java/com/interviewcoach/ai`、`ios/InterviewCoach/InterviewCoach/Features/JobBrief`。
- Assessment：主要改 `backend/src/main/java/com/interviewcoach/assessment`、`backend/src/main/java/com/interviewcoach/report`、`ios/InterviewCoach/InterviewCoach/Features/Assessment`。
- TrainingPlan：主要改 `backend/src/main/java/com/interviewcoach/training`、`ios/InterviewCoach/InterviewCoach/Features/Training`。
- MockInterview：主要改 `backend/src/main/java/com/interviewcoach/mockinterview`、`backend/src/main/java/com/interviewcoach/report`、`ios/InterviewCoach/InterviewCoach/Features/MockInterview`。
- OpenAI Provider：主要改 `backend/src/main/java/com/interviewcoach/ai`、`ios/InterviewCoach/InterviewCoach/Features/Settings`。
- Delete Account：主要改 `backend/src/main/java/com/interviewcoach/user`、`backend/src/main/java/com/interviewcoach/auth`、`ios/InterviewCoach/InterviewCoach/Core/Auth`、`ios/InterviewCoach/InterviewCoach/Core/Storage`。
- TestFlight Polish：只允许改 `ios/InterviewCoach/InterviewCoach/Core/UI`（共享组件）、`ios/InterviewCoach/InterviewCoach/Features`（各 View 替换 LoadingOverlay/ErrorBanner）、`ios/InterviewCoach/InterviewCoach/Features/Onboarding`（首次引导）、`ios/InterviewCoach/InterviewCoach/Features/Reports`（报告查看）、`ios/InterviewCoach/InterviewCoach/Features/Settings/PrivacyPolicyView.swift`（隐私政策）。
- Sign in with Apple：主要改 `backend/src/main/java/com/interviewcoach/auth`、`backend/src/main/java/com/interviewcoach/common/security`（新增 AppleTokenVerifier）、`backend/src/main/java/com/interviewcoach/common/api`（新增 AppleLoginRequest）、`backend/src/main/java/com/interviewcoach/user`（User 新增 appleUserId）、`ios/InterviewCoach/InterviewCoach/Core/Auth`、`ios/InterviewCoach/InterviewCoach/Core/API/DTO`、`ios/InterviewCoach/InterviewCoach/Features/Auth`（新建 LoginView）、`docs/api/openapi.yaml`。
- 平台默认真实 AI 接入：主要改 `backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/resources/application.yml`、`backend/src/test`、`docs/ai/provider-contracts.md`。
- CandidateProfile AI 摘要：主要改 `backend/src/main/java/com/interviewcoach/profile`、`backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/java/com/interviewcoach/common/api`、`docs/ai/prompt-contracts.md`、`docs/privacy/data-policy.md`。
- AI Prompt 契约补齐：只允许改 `docs/ai/prompt-contracts.md`、`docs/ai/provider-contracts.md`、`docs/product/vibecoding-development-plan.md`。
- AI 质量迭代：主要改 `backend/src/main/java/com/interviewcoach/ai`、对应业务模块的 Prompt 组装服务、`backend/src/test`、`docs/ai/prompt-contracts.md`。
- 开发环境真实 AI 基线：主要改 `backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/resources/application.yml`、`ios/InterviewCoach/InterviewCoach/Features/Settings`、`docs/ai/provider-contracts.md`、`README.md`。
- 真实 AI 验收样例集：主要改 `backend/src/test`、`docs/ai/prompt-contracts.md`、`docs/product/vibecoding-development-plan.md`。
- 固定 5 题结构化测评与逐题评分：主要改 `backend/src/main/java/com/interviewcoach/assessment`、`backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/java/com/interviewcoach/common/api`、`ios/InterviewCoach/InterviewCoach/Features/Assessment`、`docs/api/openapi.yaml`、`docs/ai/prompt-contracts.md`。
- 教练记忆与用户纠错：主要改 `backend/src/main/java/com/interviewcoach/coachingmemory`（新增模块）、`backend/src/main/java/com/interviewcoach/common/api`、`ios/InterviewCoach/InterviewCoach/Features/Reports`、`ios/InterviewCoach/InterviewCoach/Core/Storage`、`docs/privacy/data-policy.md`。
- 自适应专项训练：主要改 `backend/src/main/java/com/interviewcoach/training`、`backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/java/com/interviewcoach/coachingmemory`、`ios/InterviewCoach/InterviewCoach/Features/Training`、`docs/api/openapi.yaml`。
- 自适应模拟面试增强与本地记忆策略：主要改 `backend/src/main/java/com/interviewcoach/mockinterview`、`backend/src/main/java/com/interviewcoach/coachingmemory`、`ios/InterviewCoach/InterviewCoach/Features/MockInterview`、`ios/InterviewCoach/InterviewCoach/Features/Settings`、`docs/privacy/data-policy.md`。
- Spring AI Observability 与质量基线：主要改 `backend/src/main/java/com/interviewcoach/ai`、`backend/src/main/resources/application.yml`、`docs/ai/provider-contracts.md`、`docs/ai/spring-ai-long-term-foundation-plan.md`。
- 真实 AI 回归评测集升级：主要改 `backend/src/test`、`docs/ai/prompt-contracts.md`、`docs/product/vibecoding-development-plan.md`。
- 多天训练计划：主要改 `backend/src/main/java/com/interviewcoach/training`、`ios/InterviewCoach/InterviewCoach/Features/Training`、`docs/api/openapi.yaml`、`docs/ai/prompt-contracts.md`。
- 能力维度深度分析：主要改 `backend/src/main/java/com/interviewcoach/assessment`、`backend/src/main/java/com/interviewcoach/coachingmemory`、`ios/InterviewCoach/InterviewCoach/Features/Reports`、`docs/api/openapi.yaml`。
- 教练进步追踪 Dashboard：主要改 `backend/src/main/java/com/interviewcoach/progress`、`ios/InterviewCoach/InterviewCoach/Features/Progress`、`ios/InterviewCoach/InterviewCoach/Core/API/DTO`、`docs/api/openapi.yaml`。
- Chat Memory 上下文管理：主要改 `backend/src/main/java/com/interviewcoach/mockinterview`、`backend/src/main/java/com/interviewcoach/ai`、`docs/ai/spring-ai-long-term-foundation-plan.md`。
- 多轮模拟面试：主要改 `backend/src/main/java/com/interviewcoach/mockinterview`、`ios/InterviewCoach/InterviewCoach/Features/MockInterview`、`docs/api/openapi.yaml`、`docs/ai/prompt-contracts.md`。
- 发布硬化与记忆导入审查：主要改 `ios/InterviewCoach/InterviewCoach/Features/Settings`、`ios/InterviewCoach/InterviewCoach/Core/Storage`、`ios/InterviewCoach/InterviewCoach/Core/Auth`、`docs/privacy/data-policy.md`、`README.md`。

禁止为了一个任务横跨无关模块做大改。若确实需要跨越上述边界，必须先说明原因、风险和替代方案，并等待用户确认。

## 6. API 与 DTO 约束

所有后端向 iOS 返回的 JSON 必须使用 camelCase 小驼峰命名。

后端必须遵守：

- 必须返回强类型 DTO。
- 必须让 DTO 字段与 OpenAPI 文档一致。
- 必须在 Controller 或 Service 边界把 AI 结构化输出解析为强类型 DTO。
- 禁止直接把 AI 原始字符串透传给 iOS。
- 禁止返回 snake_case JSON 给 iOS。
- 禁止让业务接口返回临时 Map 结构来绕过 DTO。

iOS 必须遵守：

- Swift `Codable` 模型字段必须与后端 camelCase JSON 一一对应。
- 禁止 iOS 解析 AI 原始文本。
- 禁止 iOS 依赖不稳定的自然语言字段。

统一错误响应必须至少包含：

```json
{
  "code": "string",
  "message": "string",
  "requestId": "string"
}
```

AI 结构化输出解析失败时，后端最多重试 1 次；仍失败必须返回：

```json
{
  "code": "AI_PARSE_FAILED",
  "message": "AI returned invalid structured output.",
  "requestId": "req_xxx"
}
```

## 7. 认证与安全约束

开发阶段使用 dev login，TestFlight 前接入 Sign in with Apple。

认证 API：

- `POST /api/auth/dev-login`
- `POST /api/auth/apple`
- `POST /api/auth/logout`
- `GET /api/me`
- `DELETE /api/me`

认证实现必须遵守：

- 后端必须使用 Spring Security 建立标准 Bearer Token 拦截器链。
- `POST /api/auth/dev-login` 必须根据传入的虚拟用户名签发包含用户 ID 的 JWT 或等价 Token。
- 之后所有业务请求必须在 `Authorization: Bearer <token>` Header 中携带 Token。
- 业务接口必须从 `SecurityContextHolder` 获取当前用户。
- 未认证请求访问业务接口必须返回 401。
- iOS 必须把 Bearer Token 保存到 Keychain。
- `DELETE /api/me` 删除远端用户数据后，iOS 必须清空 Keychain 和远端同步缓存。
- Post-MVP 本机 `CoachingMemoryArchive` 是用户设备上的本地教练记忆归档，删除账号时默认保留；只有用户勾选“同时删除本机教练记忆文件”才允许删除。

禁止：

- 禁止在 Controller 或 Service 中硬编码用户 ID。
- 禁止为了开发方便绕过认证。
- 禁止使用假的全局当前用户。
- 禁止业务接口不校验用户归属。
- 禁止不同用户访问彼此的 Target、Profile、Report、Provider。

## 8. 隐私与数据存储边界

简历原文是敏感数据。默认只保存在 iOS 本地。

纯本地 SwiftData 存储：

- 简历原文。
- 用户未确认上传的项目经历原文。
- 本地草稿。
- 页面缓存状态。
- Post-MVP 本机 `CoachingMemoryArchive` 教练记忆归档。

远端 PostgreSQL 存储：

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
- 用户删除账号后，远端数据必须删除，Keychain 和远端同步缓存必须清空。
- 本机 `CoachingMemoryArchive` 默认保留，除非用户明确勾选同时删除；重新登录或重新注册时不得自动上传本机历史记忆，必须用户主动确认导入。

## 9. 简历摘要隐私链路

MVP 不做 iOS 本地 AI 摘要。

摘要生成流程必须是：

1. 用户在 iOS 粘贴简历或项目经历，原文只存本地。
2. 用户点击“生成摘要”前，App 必须明确提示：原文将临时发送到后端 AI 进行摘要生成，不会落库。
3. 后端 `POST /api/profiles/draft-summary` 接收原文。
4. 后端只允许在内存中使用原文调用 AI。
5. 后端不得保存原文，不得记录原文日志。
6. 后端返回 `CandidateProfileDraftDto`。
7. 用户在 iOS 编辑并确认摘要。
8. iOS 调用 `POST /api/profiles/confirm`。
9. 后端只保存确认后的 `CandidateProfile` 摘要。

日志强约束：

- `profiles/draft-summary` 的 Controller、Service、AI adapter 中，严禁将简历原文输出到 `System.out`、`System.err` 或任何日志框架。
- 禁止 `logger.info`、`logger.debug`、`logger.warn`、`logger.error` 记录 `resumeText`、`rawResume`、`projectRawText`、`originalText` 或任何原文字段。
- 禁止把简历原文写入 PostgreSQL、Redis、文件、缓存、审计表。
- 允许记录 requestId、userId、字符长度、处理耗时。
- 完成 CandidateProfile 相关任务前，必须搜索 Controller、Service、AI adapter，确认没有原文日志输出代码。

## 10. AI Provider 约束

当前只支持以下 Provider 类型：

- `platformDefault`
- `userOpenAICompatible`

OpenAI-compatible 配置字段：

- `name`
- `baseUrl`
- `apiKey`
- `model`
- `openaiApiMode`: `responses` 或 `chatCompletions`

Provider API：

- `GET /api/ai-providers`
- `GET /api/ai-providers/status`
- `POST /api/ai-providers`
- `POST /api/ai-providers/test`
- `PATCH /api/ai-providers/{id}/default`
- `DELETE /api/ai-providers/{id}`

安全要求：

- API Key 必须后端加密保存。
- API Key 禁止返回给 iOS。
- API Key 禁止写入日志。
- Authorization Header 禁止写入日志。
- Provider 调用失败时，禁止自动切换平台 AI，必须让用户确认。
- 删除 Provider 时必须删除密钥。

平台默认真实 AI 约束：

- 平台默认真实 AI 只允许采用 OpenAI-compatible 协议。
- 平台配置必须来自环境变量或部署配置，不允许提交真实密钥。
- 配置项必须使用 `IC_PLATFORM_AI_ENABLED`、`IC_PLATFORM_AI_BASE_URL`、`IC_PLATFORM_AI_API_KEY`、`IC_PLATFORM_AI_MODEL`、`IC_PLATFORM_AI_MODE`。
- 核心教练路径真实 AI 门禁使用 `IC_REQUIRE_REAL_AI_FOR_COACHING`，Task 18-25 默认开启；测试环境可显式关闭。
- 用户已配置默认 Provider 时，用户 Provider 优先于平台默认 AI。
- 平台真实 AI 未启用时，可以保留 `LocalPlatformAiClient` 作为单元测试、CI 非 live AI 回归、明确标记的离线演示和基础健康检查兜底。
- Post-MVP Real AI Adaptive Coaching 阶段，开发环境必须支持真实 AI 配置；测评出题、测评评分、训练反馈、专项训练、模拟面试追问和报告复盘不得静默走 stub。
- 平台真实 AI 启用但配置缺失时必须明确失败，不允许静默回退到本地 stub。
- 凡涉及 JobBrief、Assessment、Training、MockInterview、CoachingMemory、AI Observability 或 Phase 3 质量闭环的产品级验收，必须显式加载 `backend/.env` 并运行真实 AI 测试，例如：
  `cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test`。
- 若因成本、耗时、外部服务故障或配额限制未运行真实 AI，最终输出必须明确说明“未完成 AI 产品能力验收”，不得用 stub 测试替代。
- Phase 3 的 Spring AI Observability 只能采集 task、provider、model、mode、latency、success/failure、parseFailed、timeout、估算 token usage 等低风险元数据；禁止采集 prompt、completion、简历原文、用户回答原文、API Key、Authorization Header 或完整请求头。

禁止实现 Anthropic Provider。Anthropic 只允许在架构中保留扩展点，不允许创建可用业务入口。

## 11. AI 结构化输出约束

所有 AI 输出必须结构化。后端必须解析为 DTO 后返回给 iOS。

`CandidateProfileDraftDto` 必须包含：

- `summary`
- `skills`
- `projects`
- `experience`
- `rawTextLength`

`JobBriefDto` 必须包含：

- `targetId`
- `roleSummary`
- `skillMap`
- `mustHaveSkills`
- `niceToHaveSkills`
- `businessContext`
- `interviewTopics`
- `candidateMatch`
- `riskAreas`
- `confidence`

`AssessmentResultDto` 必须包含：

- `assessmentId`
- `totalScore`
- `dimensions`
- `strengths`
- `weaknesses`
- `nextActions`

`TrainingFeedbackDto` 必须包含：

- `taskId`
- `score`
- `feedback`
- `problems`
- `rewrittenAnswer`
- `followUpQuestion`
- `recommendedReviewPoints`

`MockInterviewReportDto` 必须包含：

- `mockInterviewId`
- `overallScore`
- `dimensionScores`
- `summary`
- `strengths`
- `weaknesses`
- `improvedAnswers`
- `nextTrainingTasks`

分数范围必须为 0 到 100。`confidence` 范围必须为 0 到 1。

AI 生成内容必须遵守：

- 必须只基于当前 task 已提供的上下文生成内容。
- `candidateProfileDraft` 只能基于本次临时上传的简历原文和项目经历生成摘要草稿。
- JobBrief、Assessment、Training、MockInterview 必须基于 JD、岗位画像、用户确认后的经历摘要等已确认上下文生成内容。
- 可以优化表达，禁止虚构用户没有做过的项目、技术、业务结果。
- 必须区分“真实经历”“合理迁移表达”“需要用户确认”。
- 模拟面试追问必须围绕用户上一条回答。
- 禁止保存或返回 AI hidden chain-of-thought；只能保存结构化依据、题目意图、rubric、评分、反馈、用户纠错和记忆摘要。
- 教练记忆必须标注来源和可信度：`confirmed`、`observed`、`corrected`、`inferred`、`rejected`。
- 后续 Prompt 只能把 `confirmed`、`observed`、`corrected` 当作可用事实；`inferred` 只能用于追问验证，`rejected` 禁止再次作为事实使用。
- Phase 3 Chat Memory 只能用于模拟面试短窗口上下文管理；业务长期教练记忆仍由 `CoachingMemory` 和纠错可信度规则承载。

## 12. MockInterview 上下文限制

`POST /api/mock-interviews/{id}/answer` 组装 Prompt 时必须遵守：

- 最多携带最近 6 轮对话。
- 也就是最多 12 条 message。
- 完整历史可以存 PostgreSQL。
- Redis 只允许缓存当前会话必要状态。
- 禁止每次把完整历史塞给模型。
- 禁止 Prompt 随对话轮次无限增长。

## 13. Report 生命周期

Report 是统一复盘产物。

Report 来源：

- `AssessmentSession` finish 必须创建一个 `Report`，类型为 `assessment`。
- `MockInterview` finish 必须创建一个 `Report`，类型为 `mockInterview`。
- `TrainingTask` answer 只生成 `TrainingFeedback`，不创建 Report。
- MVP 禁止实现 TrainingPlan 阶段性 Report。

关系：

- `AssessmentSession` 与 `Report`：一对一。
- `MockInterview` 与 `Report`：一对一。
- `InterviewTarget` 与 `Report`：一对多。

`Report.type` 只允许：

- `assessment`
- `mockInterview`

Report API：

- `GET /api/reports/{id}`
- `GET /api/reports?targetId={targetId}`

## 14. Vibecoding 任务顺序

必须按以下顺序开发。除非用户明确改顺序，否则禁止跳跃实现。

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
13. Sign in with Apple：Apple 登录链路、TestFlight 提审前置认证。
14. 平台默认真实 AI 接入：OpenAI-compatible 平台配置，未启用时保留本地 stub。
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
26. Spring AI Observability 与质量基线：先建立 AI 调用观测、失败率和成本信号。
27. 真实 AI 回归评测集升级：扩展 live AI 质量回归、幻觉检查和结构化解析定位。
28. 多天训练计划：从 1 天扩展为默认 3 天的受控持续训练计划。
29. 能力维度深度分析：围绕 7 个稳定能力维度沉淀趋势、短板和下一步训练重点。
30. 教练进步追踪 Dashboard：展示分数趋势、维度雷达和训练完成率。
31. Chat Memory 上下文管理：用 Spring AI 短窗口记忆替代手写切片，同时保留业务教练记忆边界。
32. 多轮模拟面试：同一目标岗位支持多次模拟面试和同维度对比。
33. 发布硬化与记忆导入审查：补齐 TestFlight/App Store、删除账号和本地记忆导入验收。

## 15. 每次任务输出格式

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

## 16. Git 提交规范

每个 commit 必须使用以下格式：

```text
<type>(<scope>): <summary>

<正文：描述本次变更的背景与动机>

Agent-Task: <原始任务描述或任务 ID>
Agent-Model: <使用的模型，如 gpt-4o、gemini-2.5-pro>
Agent-Decision: <关键设计决策及理由>
Agent-Limitation: <已知局限或后续 TODO>
```

提交规则：

- `type` 必须使用 `feat`、`fix`、`docs`、`test`、`refactor`、`chore`、`build`、`ci` 之一。
- `scope` 必须描述本次变更所属模块，例如 `ios`、`backend`、`docs`、`api`、`auth`、`health`。
- `summary` 必须使用简短中文或英文说明本次变更。
- 正文必须说明本次变更的背景与动机，禁止空正文。
- `Agent-Task` 必须保留原始任务描述或任务 ID。
- `Agent-Model` 必须写明实际使用的模型名称。
- `Agent-Decision` 必须说明关键设计决策及理由。
- `Agent-Limitation` 必须说明已知局限、未完成事项或后续 TODO；如果没有，写“无”。
- 禁止使用 `git commit -m "xxx"` 提交不完整信息。
- 若一次任务需要多个 commit，每个 commit 都必须独立满足上述格式。

示例：

```text
feat(health): 建立 iOS 到后端的健康检查链路

为 Walking Skeleton 提供最小可运行切片，包含 Spring Boot health API、OpenAPI 契约和 SwiftUI 连接状态页面，方便后续任务在可验证基础上继续推进。

Agent-Task: Task 1 Walking Skeleton
Agent-Model: gpt-5
Agent-Decision: 使用 18080 作为本地后端端口，避免与本机已有 8080 进程冲突，并同步到 OpenAPI 与 iOS client。
Agent-Limitation: 仅完成 health 链路，未实现认证、业务数据和 AI 调用。
```

## 17. 最终 MVP 验收路径

Task 1-13 已完成后，MVP 功能闭环验收结论不因 Post-MVP AI 质量任务而改变。

MVP 最终只按这一条路径验收：

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

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **interview** (5624 symbols, 17007 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/interview/context` | Codebase overview, check index freshness |
| `gitnexus://repo/interview/clusters` | All functional areas |
| `gitnexus://repo/interview/processes` | All execution flows |
| `gitnexus://repo/interview/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
