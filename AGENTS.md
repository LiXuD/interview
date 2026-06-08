# AGENTS.md

@/Users/lixd/.codex/RTK.md

任何代理在修改本项目之前，必须先阅读并遵守：

`CLAUDE.md`

`CLAUDE.md` 是本项目 vibecoding 主约束文件。除非用户给出更新、更明确的指令，否则所有开发任务都必须以 `CLAUDE.md` 为准。

## 项目定位

本项目是 AI 面试教练 iOS App，不是题库 App，不是招聘投递工具，不是简历润色工具。

MVP 最窄闭环必须是：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

所有新增功能必须服务这条闭环。

当前 MVP 功能闭环已完成。MVP 之后的新增内容必须继续服务 AI 面试教练定位，并且只能按 `docs/product/vibecoding-development-plan.md` 中已批准的 Post-MVP 任务推进。Task 1-41 全部完成：MVP + Post-MVP AI 质量闭环 + Real AI Adaptive Coaching + Phase 3 持续训练伙伴与 AI 质量运营闭环 + Phase 4 持续存在的面试教练 Agent。Spring AI 底座迁移 Phase 2-6 已完成，AI 调用通过 `AiModelGateway` 统一路由到 Spring AI 或旧客户端（灰度开关 `IC_SPRING_AI_ENABLED`）。

## 硬约束

- 必须使用 `rtk` 前缀运行 shell 命令。
- 必须每次只实现一个小任务。
- 禁止一次实现多个 Phase。
- MVP 阶段禁止扩展非 MVP 功能；MVP 完成后，禁止扩展未写入已批准开发计划的功能。
- 禁止添加题库社区、招聘投递、企业端、订阅付费、多人协作、语音面试。
- 禁止实现 Anthropic 自定义 Provider；当前只支持平台默认 AI 和 OpenAI-compatible 自定义 Provider。
- 平台默认真实 AI 必须走后端 OpenAI-compatible 代理配置，禁止 iOS 直连 AI，禁止在仓库中提交平台 API Key。
- Task 18-25 阶段，开发环境也必须能连接真实 AI；测评、训练、模拟面试等核心教练路径禁止静默使用 `LocalPlatformAiClient` stub。
- Phase 3 阶段必须先补齐 AI 可观测与真实 AI 回归评测，再扩展多天训练、进步追踪和多轮模拟面试。
- 产品能力验收、阶段评估、面向客户可用性判断、AI 质量审查必须显式使用真实 AI（默认读取 `backend/.env` 中的 `IC_LIVE_AI_TEST` 与 `IC_PLATFORM_AI_*` 配置），至少运行相关 live AI smoke；重要 AI 行为变更必须运行完整 `AiContentQualityTest` 或说明未运行原因。
- stub、mock、`LocalPlatformAiClient` 和默认 CI 测试只能证明工程结构、DTO、权限、持久化和解析逻辑未坏，禁止作为 AI 产品体验、AI 输出质量或“可面向客户”的验收依据。
- 真实 AI 验收建议命令：`cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test`。若因成本、耗时、外部服务故障或配额限制未运行真实 AI，最终输出必须明确说明“未完成 AI 产品能力验收”。
- 多天训练计划只允许按 Phase 3 受控实现：默认 3 天、每天 2-4 个任务，禁止扩展为开放式课程系统或刷题系统。

## 目录规范红线

- 新增文件必须遵守 `CLAUDE.md` 中的项目目录与模块边界规范。
- 禁止在项目根目录随意创建业务代码文件。
- 禁止把 iOS、后端、文档、脚本混放。
- 禁止绕过 feature/module 边界创建临时目录。

## 后端安全红线

- 不得跳过 Spring Security。
- 必须使用 Spring Security Bearer Token 拦截器链。
- 业务接口必须从 `SecurityUtils.currentUser()` 获取当前用户（封装了 `SecurityContextHolder`）。
- 禁止在 Controller 或 Service 中硬编码用户 ID。
- 禁止不同用户访问彼此的数据。

## 隐私红线

- 简历原文默认只允许保存在 iOS 本地。
- 生成摘要时，简历原文只允许临时上传并在后端内存中使用。
- 简历原文禁止落 PostgreSQL、Redis、文件、缓存、审计表。
- 禁止记录简历原文到 `System.out`、`System.err` 或任何日志框架。
- 禁止在日志中输出 `resumeText`、`rawResume`、`projectRawText`、`originalText` 或任何原文字段。
- API Key 必须加密保存，禁止返回给 iOS，禁止写入日志。
- Post-MVP 教练记忆禁止保存简历原文、API Key 或 AI hidden chain-of-thought。
- 删除账号时远端数据必须删除；本机 `CoachingMemoryArchive` 默认保留，只有用户勾选“同时删除本机教练记忆文件”才允许删除。
- Phase 3 的 Observability 禁止采集 prompt、completion、简历原文、用户回答原文、API Key、Authorization Header 或完整请求头。

## API 与 AI 输出红线

- 所有后端返回给 iOS 的 JSON 必须使用 camelCase。
- 后端必须返回强类型 DTO。
- 禁止把 AI 原始字符串直接返回给 iOS。
- 禁止让 iOS 解析 AI 原始文本。
- AI 结构化输出必须在后端解析为 DTO。
- AI 解析失败必须返回统一错误响应，禁止返回半成品字符串。
- 教练记忆必须区分 `confirmed`、`observed`、`corrected`、`inferred`、`rejected` 来源；`inferred` 不能当事实使用，`rejected` 禁止再次作为事实使用。
- Chat Memory 只允许用于模拟面试短窗口上下文管理；业务长期教练记忆仍由 `CoachingMemory` 和纠错可信度规则承载。

## MockInterview 红线

- 后端组装模拟面试 Prompt 时，最多携带最近 6 轮，也就是 12 条 message。
- 禁止把完整模拟面试历史塞给模型。
- 禁止 Prompt 随对话轮次无限增长。

## Report 生命周期

- `AssessmentSession` finish 创建 `Report(type=assessment)`。
- `MockInterview` finish 创建 `Report(type=mockInterview)`。
- `TrainingTask` answer 只生成 `TrainingFeedback`，不创建 Report。

## Git 提交规范

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
- 正文必须说明背景与动机，禁止空正文。
- `Agent-Task` 必须保留原始任务描述或任务 ID。
- `Agent-Model` 必须写明实际使用的模型名称。
- `Agent-Decision` 必须说明关键设计决策及理由。
- `Agent-Limitation` 必须说明已知局限、未完成事项或后续 TODO；如果没有，写“无”。
- 禁止使用 `git commit -m "xxx"` 提交不完整信息。

## 任务完成输出

每次任务完成后，必须说明：

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

禁止只回复“已完成”。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **interview** (5264 symbols, 16511 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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
