# 2026-06-10 iOS / 微信小程序 / 后端服务审查报告

审查目标：复核 iOS 端、微信小程序端和后端服务是否符合已批准计划与项目约束，并通过模拟器、微信开发者工具、后端测试和真实 AI 回归验证是否达到生产发布要求。

## 总体结论

当前结论：**不能放行生产发布**。

主要原因：

- 真实 AI 产品能力验收失败：`AiContentQualityTest` 17 个用例全部失败，后端平台默认 AI 调用底层返回 `401`，业务接口对外表现为 `502 AI_PROVIDER_CALL_FAILED`。后续直接用 `.env` 中同一组 `baseUrl/apiKey/model` curl `/models` 和 `/chat/completions`，均返回 `invalid_api_key`，可确认是上游鉴权、账号权限、模型权限或 key 配置问题。
- iOS 模拟器可编译、可启动、可进入目标详情和岗位画像缓存页，但首页显示 `未连接`，点击“重新生成岗位画像”未观察到新的成功 AI 结果，不能判定真实 AI 路线通过。
- 微信开发者工具已确认在 `/Applications/wechatwebdevtools.app`，CLI 登录与打开项目成功，但 Computer Use 无法稳定取得 DevTools 主窗口句柄；本轮未完成小程序端 GUI 输入登录与真实 AI 交互的完整自动化回归。
- 微信小程序 trial/release 后端 API 地址仍未配置真实生产域名。本轮已改为 fail closed，避免正式版回退 localhost，但发布仍需要配置真实后端域名。

已完成的修复：

- 后端 `dev-login` 默认改为关闭，只能通过 `IC_DEV_LOGIN_ENABLED=true` 显式开启。
- 微信小程序 Dev Login 仅在 `develop` 环境显示和执行，trial/release 环境隐藏并阻断。
- 微信小程序 trial/release API 地址为空时会直接报错，不再回退到本机开发地址。
- 微信小程序启动时去除重复 `reLaunch('/pages/login/login')`，登录页自行处理已登录跳转，降低 DevTools 启动期重复路由风险。
- 微信小程序目标详情页补齐核心教练闭环入口：候选人摘要、岗位画像、技术测评、训练计划、模拟面试、复盘报告。
- 后端主配置增加 `IC_DB_PASSWORD`、`IC_JWT_SECRET`、`IC_AI_ENCRYPTION_KEY` 环境变量覆盖，避免生产环境被迫使用仓库内开发默认值。
- 增加后端测试覆盖 `dev-login` 关闭时 `/api/auth/dev-login` 返回 `401 UNAUTHORIZED`。

## 审查与验证证据

### 后端

通过：

- `rtk mvn -q test`：通过。
- `rtk mvn -q -Dtest=AuthControllerTest,DevLoginDisabledSecurityTest test`：通过。
- `rtk git diff --check`：通过。

失败：

- `rtk /bin/zsh -lc 'set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test'`：失败。
- 结果：17 个测试、17 个失败、0 个 skipped。
- 共同失败表现：期望 HTTP 200，实际 HTTP 502。
- 后端错误码：`AI_PROVIDER_CALL_FAILED`。
- 底层原因：Spring AI OpenAI-compatible 调用返回 `401`。
- 失败模型上下文：`provider=platformDefault model=deepseek-v4-pro mode=chatCompletions`。

配置核对：

- `.env` 中 `IC_LIVE_AI_TEST`、`IC_SPRING_AI_ENABLED`、`IC_PLATFORM_AI_BASE_URL`、`IC_PLATFORM_AI_API_KEY`、`IC_PLATFORM_AI_MODEL` 均存在。
- `IC_PLATFORM_AI_ENABLED=true`，`IC_PLATFORM_AI_MODE=chatCompletions`。
- 配置绑定名与 `application-live-ai-test.yml` 一致，失败更像是上游鉴权、账号权限、模型权限或 baseUrl/key 组合问题。
- 直接请求平台 AI：
  - `GET $IC_PLATFORM_AI_BASE_URL/models`：HTTP `401`，返回 `invalid_api_key`。
  - `POST $IC_PLATFORM_AI_BASE_URL/chat/completions`：HTTP `401`，返回 `invalid_api_key`。

生产风险：

- 按项目红线，真实 AI 验收失败时禁止声明“AI 产品能力验收完成”。
- 当前不能面向客户承诺岗位画像、测评、训练反馈、模拟面试和 Agent 决策质量。
- 正式部署必须显式配置 `IC_DB_PASSWORD`、`IC_JWT_SECRET`、`IC_AI_ENCRYPTION_KEY`，不能使用开发默认值。

### iOS 端

验证方式：

- XcodeBuildMCP profile：`ios-review`。
- 项目：`ios/InterviewCoach/InterviewCoach.xcodeproj`。
- Scheme：`InterviewCoach`。
- 模拟器：`iPhone 17`，UDID `0BB88F21-28B0-4E2D-AEF6-A738402FAD1F`。

通过：

- `build_run_sim` 成功，bundle id `com.lixd.interviewcoach.debug`。
- App 可启动，首页显示目标列表。
- 可进入 `Senior Java Backend Engineer` 目标详情。
- 可进入岗位画像页，缓存内容可展示。
- 岗位画像页可见结构化内容：岗位概览、置信度、技能项、当前水平和补充建议。

未通过/未完成：

- 首页状态显示 `未连接`。
- 点击“重新生成岗位画像”后，未观察到新的成功 AI 生成结果。
- runtime log 未出现业务错误，仅看到系统字体 fallback；但结合 live AI 401，iOS 真实 AI 路线不能判定通过。
- 本轮未完整走完 iOS 的 5 题测评、1 天训练计划、文字模拟面试、报告全闭环。

### 微信小程序端

验证方式：

- 微信开发者工具路径：`/Applications/wechatwebdevtools.app`。
- CLI project：`miniprogram/interview-coach`。
- IDE HTTP 服务：`http://127.0.0.1:44388`。

通过：

- `rtk /Applications/wechatwebdevtools.app/Contents/MacOS/cli islogin --port 44388`：返回 `{"login":true}`。
- `rtk /Applications/wechatwebdevtools.app/Contents/MacOS/cli open --project ... --lang zh`：成功打开项目。
- `rtk /Applications/wechatwebdevtools.app/Contents/MacOS/cli auto --project ... --trust-project`：成功启用 DevTools auto。
- DevTools 问题面板此前观察为 0 issues。
- 小程序所有 `.js` 文件 `node --check` 通过。
- release 环境本地 stub 验证通过：`BASE_URL === ''` 时 request 直接拒绝，错误信息为“当前环境未配置 API 地址，请联系管理员”，且不会调用 `wx.request`。
- `project.config.json` 显示未启用 `nodeModules`，项目无小程序 npm 构建需求。
- 目标详情页已补齐主闭环入口，避免已有目标只能回到候选人摘要页。

未通过/未完成：

- Computer Use 对 `com.tencent.webplusdevtools` 返回 `cgWindowNotFound`，无法继续稳定读取 DevTools 主窗口。
- `cua-driver list_windows` 未能列出微信开发者工具窗口，GUI 自动化层仍不可用。
- 由于 GUI 自动化不可用，本轮未完成小程序 Dev Login 输入、页面流转和真实 AI 交互的完整运行态回归。
- `cli engine build` 在当前 DevTools HTTP 服务返回 `Cannot GET /engine/build`，该本地接口不可用于本版本编译验证。
- `preview/upload` 会进入上传链路，本轮未在未授权情况下执行。

生产风险：

- trial/release API URL 仍为空，正式发布前必须配置真实 HTTPS 后端域名并通过微信域名校验。
- 小程序真实 AI 路线未完成端到端验证。

## 本轮改动文件

- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/interviewcoach/auth/controller/AuthControllerTest.java`
- `miniprogram/interview-coach/app.js`
- `miniprogram/interview-coach/pages/login/login.js`
- `miniprogram/interview-coach/pages/login/login.wxml`
- `miniprogram/interview-coach/pages/target-detail/target-detail.js`
- `miniprogram/interview-coach/pages/target-detail/target-detail.wxml`
- `miniprogram/interview-coach/pages/target-detail/target-detail.wxss`
- `miniprogram/interview-coach/utils/config.js`
- `miniprogram/interview-coach/utils/request.js`
- `docs/research/audit-2026-06-10-ios-miniprogram-backend.md`

## GitNexus 影响检测

- `getBaseUrl`：LOW。
- `onDevLogin`：LOW。
- `app.js::onLaunch`：LOW。
- `utils/request.js::request`：CRITICAL 级公共入口提示，已按要求告知风险；实际改动为 fail closed 前置保护，不改变成功请求路径。
- `detect_changes(scope=all)`：medium，主要影响小程序公共 request 封装对应的 API 调用入口。
- 针对 `target-detail` 的新增入口，GitNexus 旧索引无法找到对应小程序符号；尝试 `rtk npx gitnexus analyze` 重新索引失败，错误为 `FTS extension unavailable` / `Trying to insert into an index on table File but its extension is not loaded`。因此该部分影响判断以页面级源码检查和静态验证为准。

## 发布前必须完成

1. 修复 `backend/.env` 中平台 AI 的鉴权/模型权限/baseUrl/key 组合问题，重新运行并通过完整 `AiContentQualityTest`。
2. 为微信小程序 trial/release 配置真实 HTTPS 后端 API 地址，并完成微信域名校验。
3. 恢复或改用可稳定操作的微信开发者工具自动化方式，完整跑通小程序登录、目标岗位、简历摘要确认、岗位画像、测评、训练、模拟面试和报告路径。
4. iOS 端在后端真实 AI 可用后重新跑完整闭环，而不是仅验证缓存岗位画像。
5. 真实 AI 验收通过前，不能声明“可发布生产环境”。
