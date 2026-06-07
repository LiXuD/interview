# 微信小程序入口开发计划

本文档定义 Interview Coach 微信小程序入口的产品边界、架构约束和分阶段开发任务。所有实现任务必须同时遵守项目根目录的 `AGENTS.md`、`CLAUDE.md` 和 `docs/product/vibecoding-development-plan.md`。

## 1. 定位与边界

微信小程序是 Interview Coach 的第二个用户入口，与 iOS App 共享同一个后端、同一套业务 API、同一套 AI Provider 路由和同一套隐私约束。

小程序不是独立新产品，不是题库 App，不是刷题社区，不是招聘投递工具，不是简历润色工具，不是企业端面试系统。所有页面和能力必须继续服务 AI 面试教练闭环：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 训练计划 -> 文字模拟面试 -> 报告 -> 持续教练下一步
```

首版目标是完整 iOS 等价，但执行必须按小任务拆分，禁止一次实现多个 Phase。

## 2. 技术与目录

技术栈：

- 原生微信小程序。
- WXML / WXSS / JavaScript。
- `wx.request` + 统一请求封装。
- `wx.setStorageSync` / `wx.getStorageSync` 保存非敏感本地状态。
- 后端 Bearer Token 认证。

计划目录：

```text
miniprogram/
└── interview-coach/
    ├── app.js
    ├── app.json
    ├── app.wxss
    ├── sitemap.json
    ├── pages/
    ├── components/
    ├── utils/
    │   ├── api.js
    │   ├── request.js
    │   ├── auth.js
    │   ├── config.js
    │   └── storage.js
    └── assets/
```

目录规则：

- 小程序代码只允许放在 `miniprogram/interview-coach/`。
- 接口路径集中维护在 `utils/api.js`。
- 请求、Token 注入、401 处理集中维护在 `utils/request.js`。
- 本地存储读写集中维护在 `utils/storage.js`，不得散落在页面中。
- 小程序禁止直接调用 AI，所有模型能力必须通过后端 `AiModelGateway`。

## 3. 功能范围

首版按完整 iOS 等价规划：

- 登录：开发期 Dev Login，生产小程序微信登录。
- 目标岗位：列表、创建、详情、更新、删除。
- 候选人摘要：本地粘贴简历或项目经历、显式同意临时上传、确认结构化摘要。
- 岗位画像：生成和查看 `JobBrief`。
- 测评：开始 5 题测评、提交答案、完成评分、查看测评报告。
- 训练：生成训练计划、完成训练任务、自适应专项训练。
- 模拟面试：开始文字模拟面试、回答追问、完成并查看报告。
- 报告：报告列表、报告详情、维度分析。
- 进步追踪：展示目标岗位维度趋势和训练完成情况。
- 持续教练 Agent：展示当前教练状态、下一步推荐和最近决策摘要。
- AI Provider：查看运行状态，创建、测试、设置默认、删除 OpenAI-compatible Provider。
- AI 用量：查看当前用户 AI token 用量摘要、趋势、任务/模型/Provider 维度拆分。
- 教练记忆：查看、纠错、导入本地教练记忆归档。
- 设置与隐私：隐私政策、退出登录、删除账号。

明确不做：

- 题库社区。
- 招聘投递。
- 企业端。
- 订阅付费。
- 多人协作。
- 语音面试。
- Anthropic 自定义 Provider。
- 开放式课程系统或刷题系统。

## 4. 认证与 API

小程序生产登录使用微信登录：

- 小程序调用 `wx.login()` 获取 `code`。
- 小程序调用 `POST /api/auth/wechat`。
- 后端用 `code2session` 换取微信 `openId` / `unionId`。
- 后端查找或创建用户并签发项目现有 JWT。
- 小程序后续请求使用 `Authorization: Bearer <token>`。

开发期可保留 Dev Login：

- 仅本地或测试环境使用 `POST /api/auth/dev-login`。
- 生产小程序入口不得依赖 Dev Login。

新增后端配置：

- `IC_WECHAT_LOGIN_ENABLED`
- `IC_WECHAT_MINI_PROGRAM_APP_ID`
- `IC_WECHAT_MINI_PROGRAM_APP_SECRET`
- `IC_WECHAT_CODE2SESSION_BASE_URL`

安全要求：

- 微信 `sessionKey` 不返回小程序、不落库、不写日志。
- `openId` / `unionId` 仅作为认证标识使用。
- 所有业务接口继续从 `SecurityUtils.currentUser()` 获取当前用户。
- 不新增绕过 Spring Security 的接口。

## 5. 隐私与本地存储

小程序本地允许保存：

- Bearer Token。
- 当前用户基础信息。
- 页面草稿和非敏感 UI 状态。
- 简历原文或项目经历原文的本机草稿。
- 本地教练记忆归档的结构化摘要和导入状态。

小程序本地禁止保存：

- API Key。
- 微信 `sessionKey`。
- AI hidden chain-of-thought。
- 后端返回之外的 AI 原始字符串。
- 未经用户确认上传的远端教练记忆。

简历摘要链路：

1. 用户在小程序粘贴简历或项目经历，原文只保存为本机草稿。
2. 点击生成摘要前必须明确提示：原文会临时发送到后端 AI 生成摘要，不会落库。
3. 后端只在内存中使用原文生成 `CandidateProfileDraftDto`。
4. 用户确认后，小程序调用 `POST /api/profiles/confirm`。
5. 后端只保存确认后的摘要。

删除账号：

- `DELETE /api/me` 删除远端数据。
- 小程序清空 Token、远端同步缓存和普通草稿。
- 本地教练记忆归档默认保留。
- 只有用户勾选“同时删除本机教练记忆”时，才删除本地教练记忆归档。

## 6. 分阶段任务

### MP Task 1: 文档与目录边界

目标：建立小程序入口的正式计划、API 契约和目录边界。

范围：

- 新增本计划文档。
- 更新产品、架构、隐私、OpenAPI 和 README。
- 不创建小程序代码目录。

验收：

- 文档明确小程序是第二入口，不改变产品定位。
- OpenAPI 中存在 `POST /api/auth/wechat` 契约。
- 没有计划外功能扩展。

### MP Task 2: 微信登录后端契约

目标：实现小程序生产登录所需的后端认证能力。

范围：

- 新增 `WechatLoginRequest`。
- 新增微信 code2session client。
- `AuthService` 增加微信登录。
- `User` 增加可空唯一微信身份字段。
- `SecurityConfig` 按配置放行微信登录。

验收：

- 首次微信登录创建用户。
- 重复微信登录复用用户。
- 登录后 JWT 可访问 `/api/me`。
- 微信 `sessionKey` 不返回、不落库、不写日志。

### MP Task 3: 小程序基础客户端框架

目标：建立小程序壳、请求封装、认证状态和基础导航。

范围：

- 创建 `miniprogram/interview-coach/` 标准目录。
- 实现 `utils/api.js`、`utils/request.js`、`utils/auth.js`、`utils/storage.js`。
- 实现登录页、目标列表空状态、设置入口、健康检查状态。

验收：

- Dev Login 可获取 Token 并访问 `/api/me`。
- 401 自动清理登录态并返回登录页。
- Token 注入、错误处理和 loading 状态统一。

### MP Task 4: 核心闭环页面

目标：在小程序走通核心 AI 面试教练闭环。

范围：

- 目标岗位。
- 简历摘要确认。
- 岗位画像。
- 5 题测评。
- 训练计划和训练任务。
- 文字模拟面试。
- 报告。

验收：

- 小程序可完整走通核心闭环。
- AI 输出只展示后端 DTO。
- 简历原文隐私提示和临时上传确认完整。

### MP Task 5: Post-MVP 等价能力

目标：补齐 iOS 已有 Post-MVP 用户能力。

范围：

- 自适应训练。
- 多轮模拟面试。
- 维度分析。
- 进步追踪 Dashboard。
- Coach Agent。
- AI 用量。
- AI Provider 设置。
- 教练记忆查看、纠错和导入。

验收：

- 页面能力与 iOS 等价。
- 不展示 prompt、completion、API Key 或内部 metrics。
- Agent 入口不扩展为通用聊天页。

### MP Task 6: 隐私、真实 AI 与发布验收

目标：确认小程序入口可用于真实 AI 产品体验验证。

范围：

- 小程序端隐私提示和删除账号流程。
- API Key 本地存储检查。
- 真实 AI smoke 或完整 `AiContentQualityTest`。
- 微信开发者工具真机预览。

验收：

- 核心教练路径使用真实 AI。
- 未运行 live AI 验收时，最终输出必须明确“未完成 AI 产品能力验收”。
- 本地存储不包含 API Key、微信 `sessionKey`、AI 原始字符串或隐私原文上传副本。

## 7. 测试与验收命令

文档检查：

```bash
rtk rg -n "微信小程序|mini|wechat|miniprogram|iOS App|iOS \\+" docs README.md CLAUDE.md
```

后端微信登录测试：

```bash
cd backend
rtk mvn -q -Dtest=AuthControllerTest test
```

真实 AI 产品能力验收：

```bash
cd backend
set -a
source .env
set +a
rtk mvn -q -Dtest=AiContentQualityTest test
```

若因成本、耗时、外部服务或配额限制未运行真实 AI，交付说明必须写明“未完成 AI 产品能力验收”。
