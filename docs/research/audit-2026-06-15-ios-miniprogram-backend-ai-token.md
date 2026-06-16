# 2026-06-15 iOS / 微信小程序 / 后端真实 AI 与 Token 统计审查报告

## 结论

本次审查按 `AGENTS.md`、`CLAUDE.md` 和产品计划执行，使用真实 AI 配置与本机 PostgreSQL 运行态验证。总体结论：

- 后端核心真实 AI 闭环已完成并通过 live AI 测试门禁。
- iOS 端不是只编译通过：已在模拟器中完成构建、登录、创建目标、触发真实 AI 简历摘要，并在 PostgreSQL `ai_usage_logs` 中看到记录。
- 微信小程序端已通过微信开发者工具 CLI 打开、自动化启用和 preview 编译，源码覆盖计划中的核心闭环与 Post-MVP 页面；但本次未完成小程序页面级自动点击闭环。
- Web admin token 统计页已优化，新增直接可见的 Token 构成和 Provider 消耗，不再只依赖图表 hover/抽屉查看具体消耗。

审查发现 3 个需要跟进的问题，见下方 Findings。

## Findings

### P1: `POST /api/targets` 缺少请求校验，错误字段会返回 500

证据：

- 手工 HTTP 调用使用错误字段 `jobDescription` 而不是 DTO 要求的 `jd`：
  - 返回：`HTTP/1.1 500`
  - 响应：`{"code":"INTERNAL_ERROR","message":"An unexpected error occurred",...}`
- 后端 DTO 为 `InterviewTargetCreateRequest(String title, String jd)`。

影响：

- iOS/小程序/API 调用方字段写错或缺失时，服务端返回 500，不符合客户端可恢复错误预期。
- 这不是安全绕过，但会降低端到端联调可诊断性，也不符合强契约 DTO 的产品工程要求。

建议：

- 为创建/更新目标、profile draft 等入口补 Bean Validation。
- 缺少 `title` / `jd` 时返回统一 400 错误，例如 `VALIDATION_FAILED`。

### P2: live AI 质量测试通过，但日志暴露 AI 解析和外部超时风险

证据：

- `AiContentQualityTest` 结果：17 tests, 0 failures, 0 errors, 0 skipped。
- 测试期间日志出现：
  - Spring AI read timeout 后 retry。
  - `BeanOutputConverter` 无法解析一段 `CoachingMemoryDto` 输出；模型返回重复字段和不匹配结构。

影响：

- 质量门禁通过，说明当前测试未把该解析失败升级为失败。
- 但对真实用户而言，这类解析失败可能导致教练记忆缺失或体验不稳定。

建议：

- 针对 CoachingMemory 结构化输出增加更严格 live AI case。
- 记录 parse failure rate，并在后台 token/质量运营页显示按 task 的解析失败趋势。

### P2: iOS 简历摘要真实 AI 请求成功落库，但 UI 未明显跳转到摘要确认结果

证据：

- iOS 模拟器完成：
  - build + run 成功。
  - dev login 成功，主界面显示“已连接”。
  - 创建目标成功，列表显示 `IOS Review AI Backend Engineer`。
  - 简历页显示隐私提示与二次确认弹窗。
  - 点击“同意并生成”后，PostgreSQL 最新记录出现：
    - `task=candidateProfileDraft`
    - `provider_type=platformDefault`
    - `model=sensenova-6.7-flash-lite`
    - `total_tokens=3557`
    - `success=true`
- 但模拟器 UI snapshot 仍停留在输入表单区域，未明显显示摘要确认区域。

影响：

- 后端真实 AI 和落库已证实可用。
- iOS 可能存在结果区域位置/滚动/状态提示不清晰的问题，需要补交互确认。

建议：

- 生成摘要成功后自动滚动/切换到摘要确认区域。
- 增加成功提示或明确 loading 状态结束后的焦点变化。

## 验证证据

### 后端 live AI

命令：

```bash
cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiLiveSmokeTest test
cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test
```

结果：

- `AiLiveSmokeTest`: 2 tests, 0 failures, 0 errors, 0 skipped, 157.3s。
- `AiContentQualityTest`: 17 tests, 0 failures, 0 errors, 0 skipped, 1020s。
- `backend/.env` 已确认：
  - `IC_LIVE_AI_TEST=true`
  - `IC_PLATFORM_AI_ENABLED=true`
  - `IC_PLATFORM_AI_BASE_URL` 已配置
  - `IC_PLATFORM_AI_API_KEY` 已配置
  - `IC_PLATFORM_AI_MODEL=sensenova-6.7-flash-lite`
  - `IC_PLATFORM_AI_MODE=chatCompletions`

### PostgreSQL / Token usage

后端以真实 HTTP 服务启动，日志显示连接 PostgreSQL 18.1。

运行态 HTTP 验证：

- dev login
- 创建目标
- 确认 profile
- 生成 JobBrief
- 查询当前用户 usage summary
- 查询 admin overview/users

关键结果：

- `review_user_1781490763`
  - `jobBriefRoleSummary` 正常返回岗位画像文本。
  - `usageTotalRequests=1`
  - `usageTotalTokens=2355`
- PostgreSQL `ai_usage_logs` 最新记录：
  - `jobBrief/platformDefault/sensenova-6.7-flash-lite/2355/success=true`
  - `candidateProfileDraft/platformDefault/sensenova-6.7-flash-lite/3557/success=true`
- Admin API：
  - `overviewTotalRequests=23`
  - `overviewTotalTokens=49878`
  - `providerNames=platformDefault`
  - `matchedUsers=2`
  - `firstUserTokens=2355`

### iOS

使用 XcodeBuildMCP：

- Project: `ios/InterviewCoach/InterviewCoach.xcodeproj`
- Scheme: `InterviewCoach`
- Simulator: `iPhone 16 Pro / iOS 18.2`

结果：

- `build_run_sim`: `SUCCEEDED`
- App 启动截图/快照正常。
- dev login 进入主界面，显示“已连接”。
- 创建目标成功。
- 触发真实 AI `candidateProfileDraft`，并在 PostgreSQL usage 表记录 3557 tokens。

限制：

- 本次没有在 iOS UI 中继续完成 JobBrief -> Assessment -> Training -> MockInterview 全点击闭环；后端 live smoke 已覆盖完整核心 AI 闭环。

### 微信小程序

使用微信开发者工具 CLI：

```bash
/Applications/wechatwebdevtools.app/Contents/MacOS/cli open --project miniprogram/interview-coach --port 9421 --disable-gpu
/Applications/wechatwebdevtools.app/Contents/MacOS/cli islogin --port 9421
/Applications/wechatwebdevtools.app/Contents/MacOS/cli auto --project miniprogram/interview-coach --port 9421 --trust-project
/Applications/wechatwebdevtools.app/Contents/MacOS/cli preview --project miniprogram/interview-coach --port 9421
```

结果：

- IDE server started on `http://127.0.0.1:9421`。
- `islogin`: `{"login":true}`。
- `auto`: succeeded。
- `preview`: succeeded。
- 包体：
  - TOTAL 90.0 KB
  - main 38.3 KB
  - package-admin 11.9 KB
  - package-coach 39.8 KB

源码覆盖：

- `app.json` 已包含 login/onboarding/targets/settings，以及 coach/admin subpackages。
- `utils/api.js` 集中维护 profile/jobbrief/assessment/training/mock/report/progress/agent/provider/usage/memory 等接口。
- `utils/request.js` 统一注入 Bearer Token，401 清理登录态。

限制：

- 当前 Node/npm 运行环境无法解析 `miniprogram-automator` npm 包：小程序项目本地与当前 npm 全局前缀均未安装该包；微信开发者工具 App 内部存在 `js/common/automator` 类型声明目录，但未以 npm 包形式暴露给当前 Node 运行时。因此本次没有完成小程序 UI 自动点击核心闭环。
- 小程序真实 AI 运行态以同一后端 HTTP/API 和 token usage 数据源验证为主。

### Web Admin Token Dashboard

改动目标：

- 让 token 构成和 provider 消耗在总览页直接可见，避免只靠 hover 或用户抽屉查看。

验证：

```bash
cd web/admin && npm test
cd web/admin && npm run build
```

结果：

- Vitest: 5 files passed, 30 tests passed。
- Vite build: passed。
- 构建警告：JS chunk 超过 500 KB，属于后续代码分割优化项。

浏览器限制：

- Playwright 包可用，但默认 Chromium 未安装。
- 尝试使用系统 Chrome headless 时进程 `SIGABRT`，未完成截图级浏览器验证。

## 本次修改文件

- `web/admin/src/components/TokenCompositionPanel.tsx`
- `web/admin/src/components/__tests__/TokenCompositionPanel.test.tsx`
- `web/admin/src/pages/UsageDashboardPage.tsx`
- `web/admin/src/styles.css`
- `docs/research/audit-2026-06-15-ios-miniprogram-backend-ai-token.md`

## 后续建议

1. 修复 `POST /api/targets` 等入口的请求校验，避免 500。
2. 为 CoachingMemory 结构化输出增加 live AI 回归 case。
3. 优化 iOS 摘要生成成功后的结果呈现和自动滚动。
4. 给小程序补可由当前 Node 运行时解析的 `miniprogram-automator` 依赖或等价 E2E 测试脚本，覆盖至少 login -> target -> profile draft。
5. Web admin 后续可做 route-level code splitting，消除 Vite 大 chunk 警告。

## 修复进展

2026-06-15:

- 已修复 P1：`POST /api/targets` 创建请求启用 Bean Validation，缺少 `title` / `jd` 时返回统一 `400 VALIDATION_FAILED`，不再落到数据库约束并返回 500。
- 已修复 P2 中的 CoachingMemory 结构化输出 schema 不一致：Spring AI 结构化路径改为 AI-only 输出类型，仅要求模型返回 7 个记忆数组，`id`、`targetId`、`sourceType`、`sourceId`、`createdAt` 仍由后端补齐。
- 已补强真实 AI 回归测试：`AiContentQualityTest#javaAssessmentQuestionsHaveDistinction` 现在根据真实生成题目构造对应答案，避免动态题目与固定答案错位导致门禁误判。
- 已重新运行完整真实 AI 门禁：`AiContentQualityTest` 17 tests, 0 failures, 0 errors, 0 skipped；当前报告中未再出现 CoachingMemory/BeanOutputConverter 解析失败。
- 已修复 iOS 简历摘要成功后的结果呈现：`ProfileInputView` 在真实 AI 返回后先结束 loading，再显式导航到“确认简历摘要”页，避免只停留在输入表单区域。
- 已用 iOS 模拟器 + 真实 AI 路径验证：`candidateProfileDraft` 新增 PostgreSQL token 记录 `platformDefault / sensenova-6.7-flash-lite / 1759 tokens / success=true`，模拟器 UI 自动进入“确认简历摘要”页。
- 已修复 Agent 决策 `focusDimensions` 偶发为空或超过预算的问题：`AiStructuredOutputService` 现在在结构化输出校验层强制要求 1-3 个非空关注维度，让异常输出进入既有 validation repair 重试，而不是流入 `InterviewCoachAgentRunner` 后直接让事件失败。
- 已补充回归测试：`AiStructuredOutputServiceTest#agentDecisionRetriesWhenFocusDimensionsExceedBudget` 和 `#agentDecisionRetriesWhenFocusDimensionsAreEmpty` 覆盖超预算/空维度后的修复重试路径。
- 已重新运行 Agent 相关真实 AI 验收：`AiContentQualityTest#agentDecisionAfterAssessmentIsComplete+agentDecisionToolCallsAreWhitelisted+agentJsonUsesCamelCase` 通过。
- 已再次运行完整真实 AI 门禁：`AiContentQualityTest` 17 tests, 0 failures, 0 errors, 0 skipped；耗时 973.7s；本轮未再观察到 Agent `focusDimensions` 为空或超过预算的失败日志。
