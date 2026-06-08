# AI 面试教练

一款面向技术岗求职者的 iOS App + 微信小程序双入口 AI 面试教练。用户输入目标岗位、JD、简历或项目经历后，通过 AI 完成岗位研究、能力测评、专项训练、模拟面试和复盘报告。

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
14. 删除账号并清理远端数据、本地登录态和远端同步缓存。

## 技术方向

iOS：

- SwiftUI
- iOS 17+
- SwiftData
- Keychain
- async/await
- URLSession
- Codable DTO

微信小程序：

- 原生微信小程序
- WXML / WXSS / JavaScript
- wx.request
- 统一请求封装
- Bearer Token

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
│   │   ├── provider-contracts.md
│   │   └── spring-ai-long-term-foundation-plan.md
│   ├── architecture/
│   │   └── code-wiki.md
│   ├── privacy/
│   │   └── data-policy.md
│   └── product/
│       └── vibecoding-development-plan.md
├── ios/
│   └── InterviewCoach/
├── miniprogram/
│   └── interview-coach/ # 微信小程序入口（17 页面 + 工具层）
├── backend/
├── web/
│   └── admin/          # React 管理端（Token 用量看板）
├── infra/
└── scripts/
```

当前仓库已完成 Task 1-41：MVP 闭环（Walking Skeleton → OpenAPI → Dev Login → Target CRUD → CandidateProfile → JobBrief → Assessment → TrainingPlan → MockInterview → User Provider → Delete Account → TestFlight Polish → Sign in with Apple）、Post-MVP AI 质量闭环（Task 14-17）、Post-MVP Real AI Adaptive Coaching（Task 18-25）、Phase 3 持续训练伙伴与 AI 质量运营闭环（Task 26-33）、Phase 4 持续存在的面试教练 Agent（Task 34-41）。Spring AI 底座迁移 Phase 2-6 已完成，AI 调用通过 `AiModelGateway` 统一路由到 Spring AI 或旧客户端。Admin Token Usage Dashboard 后端和前端已完成。微信小程序入口 MP Task 1-6 已完成，包含 17 个页面、微信登录后端认证和核心 AI 面试教练闭环。

## 开发计划

主计划文档：

- [AI 面试教练 App Vibecoding 三层开发计划](docs/product/vibecoding-development-plan.md)
- [微信小程序入口开发计划](docs/product/wechat-miniprogram-development-plan.md)

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

Post-MVP Real AI Adaptive Coaching 路线：

18. 开发环境真实 AI 基线：核心教练路径禁止静默走 stub。
19. 真实 AI 验收样例集：用典型岗位样例验证真实模型输出质量。
20. 固定 5 题结构化测评：题目包含维度、难度、意图和评分 rubric。
21. 教练记忆 Coaching Memory：沉淀训练、测评、模拟面试中的结构化用户理解。
22. 用户纠错与记忆可信度：支持用户纠正 AI 判断，并标注记忆来源和可信度。
23. 逐题评分与回答结构诊断：按题诊断回答内容、结构、追问风险和改进示范。
24. 自适应专项训练会话：根据回答动态决定追问、换角度、达标或停止。
25. 自适应模拟面试增强与本地记忆策略：增强真实面试追问，并明确本地记忆保留/删除规则。

Phase 3 持续训练伙伴与 AI 质量运营闭环：

26. Spring AI Observability 与质量基线：先建立 AI 调用观测、失败率和成本信号。
27. 真实 AI 回归评测集升级：扩展 live AI 质量回归、幻觉检查和结构化解析定位。
28. 多天训练计划：从 1 天扩展为默认 3 天的受控持续训练计划。
29. 能力维度深度分析：围绕 7 个稳定能力维度沉淀趋势、短板和下一步训练重点。
30. 教练进步追踪 Dashboard：展示分数趋势、维度雷达和训练完成率。
31. Chat Memory 上下文管理：用 Spring AI 短窗口记忆替代手写切片，同时保留业务教练记忆边界。
32. 多轮模拟面试：同一目标岗位支持多次模拟面试和同维度对比。
33. 发布硬化与记忆导入审查：补齐 TestFlight/App Store、删除账号和本地记忆导入验收。

微信小程序入口路线：

1. 文档与目录边界。
2. 微信登录后端契约。
3. 小程序基础客户端框架。
4. 核心闭环页面。
5. Post-MVP 等价能力。
6. 隐私、真实 AI 与发布验收。

小程序后续本地调试将复用后端 `18080` API；生产入口使用微信登录换取项目现有 Bearer Token。

## 本地真实 AI 配置

Task 18 起，测评出题、测评评分、训练计划/反馈、模拟面试追问和报告复盘必须使用真实 AI。开发环境可以二选一：

- 在 App 的 `AI Provider` 设置页配置用户 OpenAI-compatible Provider。
- 在后端环境变量中配置平台 Provider：
  - `IC_PLATFORM_AI_ENABLED=true`
  - `IC_PLATFORM_AI_BASE_URL`
  - `IC_PLATFORM_AI_API_KEY`
  - `IC_PLATFORM_AI_MODEL`
  - `IC_PLATFORM_AI_MODE=chatCompletions` 或 `responses`

默认 `IC_REQUIRE_REAL_AI_FOR_COACHING=true`。未配置用户 Provider 且平台真实 AI 未完整配置时，核心教练入口会阻止继续；`LocalPlatformAiClient` 仅保留给单元测试、CI 非 live AI 回归、明确离线演示和基础健康检查。

## 开发约束

本项目采用 vibecoding 方式开发。任何 AI 开发代理或人工协作者在修改项目之前，必须先阅读：

- [AGENTS.md](AGENTS.md)
- [CLAUDE.md](CLAUDE.md)

关键红线：

- 每次只实现一个小任务。
- MVP 阶段禁止扩展非 MVP 功能；MVP 完成后只允许按已批准的 Post-MVP 计划推进。
- Task 18-25 阶段，开发环境也必须能连接真实 AI；测评、训练、模拟面试等核心教练路径禁止静默使用 stub。
- Phase 3 阶段必须先补齐 AI 可观测与真实 AI 回归评测，再扩展多天训练、进步追踪和多轮模拟面试。
- Phase 3 的多天训练计划必须保持受控：默认 3 天、每天 2-4 个任务，禁止扩展为开放式课程系统或刷题系统。
- 后端必须使用 Spring Security Bearer Token。
- 所有后端返回给 iOS 的 JSON 必须使用 camelCase。
- 后端必须返回强类型 DTO。
- 禁止把 AI 原始字符串直接返回给 iOS。
- 简历原文默认只保存在 iOS 本地。
- 生成摘要时，简历原文只允许临时上传并在后端内存中使用。
- 禁止记录简历原文或 API Key 到日志。
- 禁止保存或返回 AI hidden chain-of-thought；教练记忆只能保存结构化依据、题目意图、rubric、评分、反馈、用户纠错和记忆摘要。
- 模拟面试 Prompt 最多携带最近 6 轮，也就是 12 条 message。
- Spring AI Observability 禁止采集 prompt、completion、简历原文、用户回答原文、API Key、Authorization Header 或完整请求头。
- Chat Memory 只允许用于模拟面试短窗口上下文管理，业务长期教练记忆仍由 `CoachingMemory` 和纠错可信度规则承载。

## GitHub

远端仓库：

- <https://github.com/LiXuD/interview.git>

## 当前状态

Task 1-41 全部完成：MVP + Post-MVP AI 质量 + Real AI Adaptive Coaching + Phase 3 持续训练伙伴与 AI 质量运营闭环 + Phase 4 持续存在的面试教练 Agent。Spring AI 底座迁移 Phase 2-6 已完成，AI 调用已通过 `AiModelGateway` 统一路由到 Spring AI `ChatClient`（`chatCompletions` 模式）或旧客户端（`responses` 模式）。微信小程序入口 MP Task 1-6 已完成，小程序共享后端 API，支持微信登录和完整 AI 面试教练闭环。

已完成：

- Task 1-13：MVP 闭环全部打通（Walking Skeleton → OpenAPI → Dev Login → Target CRUD → CandidateProfile → JobBrief → Assessment → TrainingPlan → MockInterview → User Provider → Delete Account → TestFlight Polish → Sign in with Apple）。
- Task 14-17：Post-MVP AI 质量闭环（平台默认真实 AI 接入、CandidateProfile AI 摘要、AI Prompt 契约补齐、AI 质量迭代）。
- Task 18-25：Post-MVP Real AI Adaptive Coaching（开发环境真实 AI 基线、真实 AI 验收样例、固定 5 题结构化测评、教练记忆、用户纠错与记忆可信度、逐题评分与回答结构诊断、自适应专项训练、自适应模拟面试增强与本地记忆策略）。
- Task 26-33：Phase 3 持续训练伙伴与 AI 质量运营闭环（Spring AI Observability、真实 AI 回归评测升级、多天训练计划、能力维度深度分析、进步追踪 Dashboard、Chat Memory 上下文管理、多轮模拟面试、发布硬化与记忆导入审查）。
- Task 34-41：Phase 4 持续存在的面试教练 Agent（Agent 身份与状态、事件驱动决策、业务事件接入、下一步推荐统一、白名单工具编排、AI 调用收拢、iOS 教练入口、Agent 真实 AI 回归与隐私硬化）。
- Spring AI 底座迁移：Phase 2（依赖引入）→ Phase 3（平台 Provider 迁移）→ Phase 4（用户 Provider 迁移）→ Phase 5（结构化输出升级）→ Phase 6（Advisor 与记忆增强）。

当前架构：

```text
业务模块 → AiStructuredOutputService → AiModelGateway
  ├── SpringAiPlatformClient（平台默认 AI，chatCompletions 模式）
  ├── SpringAiUserProviderClient（用户自定义 Provider，chatCompletions 模式）
  ├── PlatformRealAiClient（平台默认 AI，responses 模式回退）
  ├── OpenAiCompatibleClient（用户 Provider，responses 模式回退）
  └── LocalPlatformAiClient（仅测试/离线演示/CI）
```

环境变量（Spring AI 相关）：

- `IC_SPRING_AI_ENABLED`：Spring AI 灰度开关，默认 `false`。设为 `true` 时平台和用户的 `chatCompletions` 模式走 Spring AI 路径。
- `IC_AI_HTTP_CONNECT_TIMEOUT_MS`：AI HTTP 连接超时，默认 5000ms。
- `IC_AI_HTTP_READ_TIMEOUT_MS`：AI HTTP 读取超时，默认 60000ms。
