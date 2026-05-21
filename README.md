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
│   └── product/
│       └── vibecoding-development-plan.md
├── ios/
│   └── InterviewCoach/
├── backend/
├── infra/
└── scripts/
```

当前仓库已完成 Task 1-4：Walking Skeleton、OpenAPI 契约与 DTO、Dev Login 认证链路、Target CRUD。后续按任务顺序继续推进 CandidateProfile 隐私链路。

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

## 开发约束

本项目采用 vibecoding 方式开发。任何 AI 开发代理或人工协作者在修改项目之前，必须先阅读：

- [AGENTS.md](AGENTS.md)
- [CLAUDE.md](CLAUDE.md)

关键红线：

- 每次只实现一个小任务。
- 禁止扩展非 MVP 功能。
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

当前阶段：Task 4 Target CRUD 已完成，下一步 Task 5 CandidateProfile 隐私链路。

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

尚未创建：

- CandidateProfile、AI、测评、训练和模拟面试业务模块。
