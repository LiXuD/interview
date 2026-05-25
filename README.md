# AI 技术岗面试教练

一款面向技术岗求职者的 iOS App。用户输入目标岗位、JD、简历或项目经历后，App 通过 AI 完成岗位研究、能力测评、专项训练、模拟面试和复盘报告。

本项目不是题库 App，也不是简历润色工具，而是一个围绕目标岗位进行针对性提升的 AI 面试教练。

## MVP 闭环

第一版只围绕一条最窄闭环开发：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

MVP 完成标准：

1. 用户登录。
2. 创建目标岗位。
3. 粘贴 JD。
4. 粘贴简历或项目经历。
5. 同意临时上传原文生成摘要。
6. 确认 `CandidateProfile` 摘要。
7. 生成 `JobBrief` 岗位画像。
8. 完成 5 题 `Assessment`。
9. 查看 `Assessment Report`。
10. 生成 1 天 `TrainingPlan`。
11. 完成 1 个 `TrainingTask`。
12. 完成 1 次文字 `MockInterview`。
13. 查看 `MockInterview Report`。
14. 删除账号并清理本地和远端数据。

## 技术方向

iOS：

- SwiftUI
- iOS 17+
- SwiftData
- Keychain
- async/await
- URLSession
- Codable DTO

后端：

- Spring Boot 3
- PostgreSQL
- Redis
- Spring Security
- OpenAPI
- 模块化单体

AI：

- 后端统一代理 AI 请求。
- MVP 默认平台 AI。
- MVP 支持 OpenAI-compatible 自定义 Provider。
- Anthropic 协议后移，不进入最窄 MVP。

## 项目结构

```text
interview/
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── docs/
│   ├── api/
│   │   └── openapi.yaml
│   ├── ai/
│   │   ├── prompt-contracts.md
│   │   └── provider-contracts.md
│   ├── privacy/
│   │   └── data-policy.md
│   └── product/
│       └── vibecoding-development-plan.md
├── ios/
│   └── InterviewCoach/
├── backend/
├── infra/
└── scripts/
```

当前仓库已完成 Task 1-13：Walking Skeleton、OpenAPI 契约与 DTO、Dev Login 认证链路、Target CRUD、CandidateProfile 隐私链路、Platform AI + JobBrief、Assessment 5 题测评、TrainingPlan 1 天任务、MockInterview 文字模拟面试、User OpenAI Provider、Delete Account、TestFlight Polish、Sign in with Apple。MVP 闭环已全部打通。

说明：MVP 阶段已打通完整功能路径，但 AI 质量仍进入 Post-MVP 增强阶段。当前平台默认 AI 仍保留本地 stub 以支持无密钥演示，真实 AI 摘要生成和平台默认真实 AI 接入将在 Task 14-17 中推进。

## 开发计划

主计划文档：

- [AI 技术岗面试教练 App Vibecoding 三层开发计划](docs/product/vibecoding-development-plan.md)

后续开发按以下顺序推进：

1. Walking Skeleton
2. OpenAPI 与基础 DTO
3. Dev Login
4. Target CRUD
5. CandidateProfile 隐私链路
6. Platform AI + JobBrief
7. Assessment 5 题测评
8. TrainingPlan 1 天任务
9. MockInterview
10. User OpenAI Provider
11. Delete Account
12. TestFlight Polish
13. Sign in with Apple

Post-MVP AI 质量路线：

14. 平台默认真实 AI 接入：OpenAI-compatible 平台配置，未启用时保留本地 stub。
15. CandidateProfile AI 摘要：`draft-summary` 接入结构化 AI 输出。
16. AI Prompt 契约补齐：记录 task、输入边界、输出 DTO 和失败策略。
17. AI 质量迭代：优化 JobBrief、Assessment、Training、MockInterview 的 Prompt 和校验。

## 开发约束

本项目采用 vibecoding 方式开发。任何 AI 开发代理或人工协作者在修改项目之前，必须先阅读：

- [AGENTS.md](AGENTS.md)
- [CLAUDE.md](CLAUDE.md)

关键红线：

- 每次只实现一个小任务。
- MVP 阶段禁止扩展非 MVP 功能；MVP 完成后只允许按已批准的 Post-MVP 计划推进。
- 后端必须使用 Spring Security Bearer Token。
- 所有后端返回给 iOS 的 JSON 必须使用 camelCase。
- 后端必须返回强类型 DTO。
- 禁止把 AI 原始字符串直接返回给 iOS。
- 简历原文默认只保存在 iOS 本地。
- 生成摘要时，简历原文只允许临时上传并在后端内存中使用。
- 禁止记录简历原文或 API Key 到日志。
- 模拟面试 Prompt 最多携带最近 6 轮，也就是 12 条 message。

## GitHub

远端仓库：

- <https://github.com/LiXuD/interview.git>

## 当前状态

当前阶段：Task 13 Sign in with Apple 已完成，MVP 闭环全部打通（Task 1-13）。

下一阶段：Post-MVP AI 质量闭环，优先补齐平台默认真实 AI 接入与 CandidateProfile AI 摘要生成。

已完成：

- Task 1：Spring Boot 后端 health API、SwiftUI iOS App 壳、OpenAPI health 契约。
- Task 2：完整 OpenAPI 契约（29 个路径、30+ 个 Schema、bearerAuth 安全方案）。
- Task 2：后端 33 个 Java record DTO（`common/api` + `common/error`）。
- Task 2：iOS 10 个 Swift Codable DTO 文件（`Core/API/DTO/`）。
- Task 3：后端 JWT 签发/验证（jjwt）、Spring Security Bearer Token 拦截器链、User 实体与 Repository。
- Task 3：后端 AuthController（dev-login、logout）和 UserController（GET/DELETE /api/me）。
- Task 3：iOS KeychainHelper、APIClient actor、AuthService（ObservableObject）、DevLoginView。
- Task 3：AppRootView auth gating、infra/docker-compose.yml（PostgreSQL）。
- Task 4：后端 InterviewTarget 实体、Repository、Service、Controller（5 个 REST 端点）。
- Task 4：后端 TargetNotFoundException、status 白名单校验、8 个集成测试。
- Task 4：iOS TargetListView、TargetCreateView、TargetDetailView。
- Task 4：iOS TargetLocal SwiftData 模型、TargetStatusHelper 共享 UI 组件。
- Task 5：后端 CandidateProfile 实体、Repository、Service（Stub draft）、Controller（3 个端点）。
- Task 5：后端 ProfileNotFoundException、隐私约束（原文内存使用、不记录日志）。
- Task 5：iOS CandidateProfileLocal SwiftData 模型、ProfileInputView、ProfileConfirmView。
- Task 5：docs/privacy/data-policy.md 隐私文档。
- Task 6：后端 PlatformAiClient 接口 + LocalPlatformAiClient（stub）、AiStructuredOutputService（generateJobBrief + retry）。
- Task 6：后端 JobBrief 实体、Repository、Service、Controller（2 个端点）、6 个集成测试。
- Task 6：iOS JobBriefView（岗位画像展示 + 重新生成）。
- Task 7：后端 AssessmentSession/AssessmentResult/Report 实体、AssessmentDimension @Embeddable。
- Task 7：后端 AssessmentService（start/answer/finish 流程 + 状态机）、AssessmentController（4 端点）。
- Task 7：后端 ReportService/ReportController、6 个集成测试。
- Task 7：iOS AssessmentView（答题流程）、AssessmentResultView（评分展示）。
- 代码清理：提取 SecurityUtils.currentUser()、AI 泛型重试方法、ReportNotFoundException。
- Task 8：后端 TrainingPlan/TrainingTask/TrainingFeedback 实体、3 个 Repository、TrainingService。
- Task 8：后端 TrainingPlanController + TrainingTaskController（4 个端点）、7 个集成测试。
- Task 8：AiStructuredOutputService 扩展（generateTrainingPlan + generateTrainingFeedback）。
- Task 8：iOS TrainingPlanView（生成/查看计划）、TrainingTaskView（答题+反馈+完成）。
- Task 9：后端 MockInterview/MockInterviewMessage 实体、2 个 Repository、MockInterviewService。
- Task 9：后端 MockInterviewController（4 个端点）、5 个集成测试。
- Task 9：AiStructuredOutputService 扩展（generateMockInterviewQuestion + generateMockInterviewReport）。
- Task 9：iOS MockInterviewView（聊天界面）、MockInterviewResultView（报告展示）。
- Task 9：InterviewTargetService + AuthService 级联删除 MockInterview。
- Task 10：后端 AiProvider 实体（@ManyToOne User）、Repository、Service、Controller（5 个端点）。
- Task 10：后端 ApiKeyEncryption（AES-GCM）、OpenAiCompatibleClient（chatCompletions + responses）。
- Task 10：后端 AiStructuredOutputService Provider 路由（SecurityContextHolder → 默认 Provider → OpenAiCompatibleClient / 平台 AI）。
- Task 10：后端 7 个集成测试（全生命周期、认证、跨用户隔离、级联删除、API Key 不泄露）。
- Task 10：iOS SettingsView、AiProviderListView（列表+默认切换+滑动删除）、AiProviderCreateView（创建表单+连接测试）。
- Task 11：后端无改动，DELETE /api/me 已在 Task 3+10 中完整实现（10 张表级联删除）。
- Task 11：iOS AuthService 新增 deleteAccount() 方法（调用 DELETE /api/me 后 logout 清除本地数据）。
- Task 11：iOS SettingsView 新增删除账号区域 + confirmationDialog 二次确认。
- Task 11：后端 9 个测试通过（deleteMeWithValidTokenReturns204 + deleteUserAfterProfileConfirmSucceeds）。
- Task 13：后端 User 实体新增 appleUserId（unique）+ email 字段。
- Task 13：后端 AppleTokenVerifier（RS256 RSA 公钥验签 + JWKS 缓存 1 小时 TTL + nonce 重放保护）。
- Task 13：后端 AppleLoginRequest DTO（identityToken + nonce 必填）、AuthService.appleLogin() + findOrCreateAppleUser()。
- Task 13：后端 AuthController POST /api/auth/apple 端点、SecurityConfig permitAll。
- Task 13：后端 54 个测试全部通过。
- Task 13：iOS LoginView（SignInWithAppleButton + CryptoKit nonce 生成 + #if DEBUG dev login 入口）。
- Task 13：iOS AuthService.appleLogin()、AppleLoginRequestDTO（含 nonce）、performLogin 通用方法。
- Task 13：iOS InterviewCoach.entitlements（com.apple.developer.applesignin array）。
- Task 13：OpenAPI 新增 /api/auth/apple 路径、AppleLoginRequest schema（nonce required）、错误响应 401。

尚未创建：

- 无（Task 1-13 全部完成）。
