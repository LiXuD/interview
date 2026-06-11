# Interview Coach 小程序审查报告

> 审查时间：2026-06-11
> 项目：interview-coach (wx2b4e01325ea67c44)
> 基础库版本：3.3.4
> 页面数量：19 个（全部在主包）

---

## 总览

| 维度 | 评分 | 说明 |
|------|------|------|
| 代码架构 | ⭐⭐⭐⭐ | 工具层设计清晰，API 集中管理，职责分离到位 |
| 微信合规 | ⭐⭐ | 隐私授权、权限声明、域名配置存在严重缺陷 |
| 性能 & 包体 | ⭐⭐ | 19 页全在主包，无分包策略，上线必爆 |
| 用户体验 | ⭐⭐⭐ | 交互流程完整，但导航设计有硬伤 |
| 安全性 | ⭐⭐⭐ | API Key 处理得当，但开发态暴露过多 |

---

## 🔴 严重问题（提审必被拒 / 上线必崩）

### 1. 19 页全部在主包，无分包策略

**位置**: `app.json`

当前所有 19 个页面都在 `pages` 数组中，未配置 `subPackages`。微信主包上限 **2MB**，19 个页面（每个 4 个文件 × 19 = 76 个文件）加上工具层和全局样式，大概率超限。

**影响**: 包体超限无法上传代码，提审直接被拒。

**修复方案**:

```json
{
  "pages": [
    "pages/login/login",
    "pages/onboarding/onboarding",
    "pages/targets/targets",
    "pages/target-create/target-create",
    "pages/target-detail/target-detail"
  ],
  "subPackages": [
    {
      "root": "package-coach",
      "name": "coach",
      "pages": [
        "pages/profile/profile",
        "pages/jobbrief/jobbrief",
        "pages/assessment/assessment",
        "pages/training/training",
        "pages/mock-interview/mock-interview",
        "pages/report-detail/report-detail",
        "pages/dimension-analysis/dimension-analysis",
        "pages/progress/progress",
        "pages/coach-agent/coach-agent",
        "pages/coaching-memory/coaching-memory"
      ]
    },
    {
      "root": "package-admin",
      "name": "admin",
      "pages": [
        "pages/settings/settings",
        "pages/privacy/privacy",
        "pages/ai-usage/ai-usage",
        "pages/ai-providers/ai-providers"
      ]
    }
  ]
}
```

> 分包后所有 `wx.navigateTo` / `wx.redirectTo` 路径需要同步更新，如 `/pages/training/training` → `/package-coach/pages/training/training`。

---

### 2. 缺少隐私合规授权机制

**位置**: 全局

自 2023 年 9 月起，微信要求所有小程序在调用隐私 API（如 `wx.login`、`wx.chooseMedia`、`wx.getClipboardData` 等）前，**必须先经过用户隐私授权**。当前项目：

- ❌ 未配置 `"__usePrivacyCheck__": true"` (app.json)
- ❌ 未调用 `wx.requirePrivacyAuthorize`
- ❌ 未使用 `wx.getPrivacySetting` 检测授权状态
- ❌ privacy 页面只是静态文案，无实际授权交互

**影响**: 提审大概率被拒；即使侥幸过审，基础库 2.32.3+ 上调用 `wx.login` 会直接弹窗或失败。

**修复方案**:

```json
// app.json 顶部添加
{
  "__usePrivacyCheck__": true
}
```

```javascript
// utils/privacy.js - 新建隐私授权工具
function authorize() {
  return new Promise((resolve, reject) => {
    wx.getPrivacySetting({
      success(res) {
        if (res.needAuthorization) {
          // 需要弹窗授权，可跳转到隐私弹窗组件
          reject(new Error('NEED_PRIVACY_AUTH'));
        } else {
          resolve();
        }
      },
      fail: reject
    });
  });
}
```

在 `app.js` 的 `onLaunch` 或 `login.js` 调用 `wx.login` 之前，先调用 `wx.requirePrivacyAuthorize`。

---

### 3. app.json 缺少 `permission` 声明

**位置**: `app.json`

项目使用了 `wx.login`，需要在 `app.json` 中声明 scope 权限。缺少声明在新版基础库上可能导致 API 调用失败。

**修复方案**:

```json
{
  "permission": {
    "scope.userLocation": {
      "desc": "你的位置信息将用于小程序位置接口的效果展示"
    }
  }
}
```

> 即使当前不用定位，`wx.login` 相关的权限声明也应补充。

---

### 4. 生产环境 API 地址为空

**位置**: `utils/config.js`

```javascript
trial: '',   // 体验版 — 空字符串
release: ''  // 正式版 — 空字符串
```

**影响**: 体验版和正式版启动后所有请求必失败（`request.js` 中 `BASE_URL` 为空会直接 reject）。

**修复方案**: 部署前必须填写实际 HTTPS 后端地址，并在微信公众平台「开发管理 → 开发设置 → 服务器域名」中注册。

---

### 5. 开发环境使用 HTTP

**位置**: `utils/config.js` 第 7 行

```javascript
develop: 'http://localhost:18080'
```

微信要求所有正式请求必须 HTTPS。开发环境虽可关闭域名校验（当前 `urlCheck: false`），但：

- 提审时 `urlCheck` 必须为 `true`
- 正式环境 HTTP 请求会被微信拦截

**修复方案**: 开发环境建议使用本地 HTTPS 代理（如 mitmproxy / nginx 自签证书），或至少确保 `trial` 和 `release` 使用 HTTPS。

---

## 🟠 重要问题（影响体验 / 存在隐患）

### 6. 自定义 Tab Bar 实现不完整

**位置**: `pages/targets/targets.wxml` + `targets.wxss`

当前 targets 页面手写了一个底部 tab-bar（目标 + 设置），但：

- ❌ `app.json` 未声明 `tabBar`
- ❌ 设置页用 `wx.navigateTo` 打开，进入后 tab-bar 消失
- ❌ 无法使用 `wx.switchTab`
- ❌ 不符合微信自定义 tabBar 规范

**修复方案**（二选一）：

**方案 A：使用微信自定义 tabBar**

```json
// app.json
{
  "tabBar": {
    "custom": true,
    "list": [
      { "pagePath": "pages/targets/targets", "text": "目标" },
      { "pagePath": "pages/settings/settings", "text": "设置" }
    ]
  }
}
```

创建 `custom-tab-bar` 组件目录。

**方案 B：保持当前方案但修复导航**

将 settings 的跳转改为 `wx.switchTab`（需先在 app.json 配置 tabBar），或接受 tab-bar 仅在首页显示的现状。

---

### 7. 线性流程过度使用 `wx.redirectTo`

**位置**: 整个教练闭环流程

当前用户流程：
```
创建目标 → redirectTo 摘要 → redirectTo 画像 → redirectTo 测评 → redirectTo 训练 → redirectTo 模拟面试
```

`wx.redirectTo` 会**关闭当前页面**，导致：

- 用户无法回退查看上一步内容（如做训练时想回看岗位画像）
- 页面栈被清空，`wx.navigateBack` 失效
- 在模拟面试中途无法返回查看训练反馈

**修复方案**: 将核心流程改为 `wx.navigateTo`，让用户可以回退。仅在明确"不可逆"的场景（如测评提交后）使用 `redirectTo`。

---

### 8. 401 并发重复跳转

**位置**: `utils/request.js` 第 42-44 行

如果页面上同时发起 3 个请求且全部返回 401，会触发 3 次 `wx.reLaunch({ url: '/pages/login/login' })`。虽然最终效果都是跳转到登录页，但可能引发闪烁或异常。

**修复方案**:

```javascript
let isRedirecting = false;

// request 函数内
if (res.statusCode === 401) {
  storage.clearAuth();
  if (!isRedirecting) {
    isRedirecting = true;
    wx.reLaunch({
      url: '/pages/login/login',
      complete: () => { isRedirecting = false; }
    });
  }
  reject(new Error('未登录或登录已过期'));
  return;
}
```

---

### 9. 没有任何分享能力

**位置**: 全局

搜索结果显示整个项目 **零处** `onShareAppMessage` 或 `onShareTimeline`。

**影响**:
- 用户无法将小程序分享给朋友或朋友圈
- 错失微信生态最大的流量入口
- 提审时可能被质疑缺乏社交属性

**修复方案**: 至少在以下页面添加分享：

- **target-detail**: 分享目标岗位的教练建议
- **report-detail**: 分享面试测评报告
- **mock-interview**（完成后）: 分享模拟面试成绩

```javascript
onShareAppMessage() {
  return {
    title: '我在用 AI 面试教练准备面试',
    path: '/pages/targets/targets',
  };
}
```

---

### 10. 隐私页面仅是静态文案

**位置**: `pages/privacy/privacy.js` — `Page({})`

privacy 页面没有 JS 逻辑，只有静态 WXML 文案。作为隐私政策展示页：

- ❌ 没有用户同意/不同意的交互
- ❌ 没有与隐私授权 API 的联动
- ❌ 文案缺少生效日期、更新日期

**修复方案**: 结合第 2 点，实现真正的隐私授权弹窗 + 隐私政策详情页。

---

### 11. `sitemap.json` 暴露所有页面

**位置**: `sitemap.json`

```json
{ "action": "allow", "page": "*" }
```

这意味着 settings、ai-providers、ai-usage 等管理页面也能被微信搜索索引。

**修复方案**:

```json
{
  "rules": [
    { "action": "allow", "page": "pages/targets/targets" },
    { "action": "allow", "page": "pages/target-detail/target-detail" },
    { "action": "disallow", "page": "pages/settings/*" },
    { "action": "disallow", "page": "pages/ai-providers/*" },
    { "action": "disallow", "page": "pages/ai-usage/*" }
  ]
}
```

---

### 12. 模拟面试聊天滚动不稳定

**位置**: `pages/mock-interview/mock-interview.wxml`

```xml
<scroll-view scroll-into-view="{{scrollToId}}">
```

`scroll-into-view` 在快速连续消息时可能不触发滚动（已知微信 bug），且 `scrollToId` 初始为空字符串。

**修复方案**:

- 使用 `scroll-top` 替代 `scroll-into-view`，动态设置一个很大的值
- 或在 setData 回调中使用 `wx.createSelectorQuery` 手动滚动

---

## 🟡 改进建议（提升质量 / 体验优化）

### 13. 添加全局登录态守卫

当前只有 login 页在 `onLoad` 检查登录态。如果用户在某个页面 Token 过期，依赖 request.js 的 401 兜底跳转。建议：

- 在 `app.js` 的 `onShow` 中检查登录态
- 或封装一个 `withAuth` 高阶函数，Page 注册前自动检查

---

### 14. setData 性能优化

多处代码存在可优化的 setData 调用：

**示例 1** — `targets.js` 连续两次 setData:
```javascript
// 当前
this.setData({ loading: true, error: '' });
// ... 异步 ...
this.setData({ targets: data });
this.setData({ loading: false });
```
→ 可以合并为更少的 setData 调用。

**示例 2** — `training.js` 的 `submitAdaptiveAnswer`:
```javascript
this.setData({ feedback, adaptiveHistory: finalHistory, taskAnswer: '' });
this.loadPlan();  // loadPlan 内又会触发多次 setData
```
→ 两次渲染合并。

---

### 15. wx.login 回调风格 → Promise

**位置**: `utils/auth.js`

当前使用回调风格：
```javascript
wx.login({
  success(loginRes) { ... },
  fail(err) { ... }
});
```

基础库 2.10.2+ 支持原生 Promise，建议改为：
```javascript
const { code } = await wx.login();
```

---

### 16. 骨架屏提升首屏感知

当前所有页面加载状态为纯文字 "加载中..."。微信支持原生骨架屏（`skeleton` 配置），建议为核心页面（targets、target-detail、training）添加骨架屏。

---

### 17. 网络请求增加重试机制

当前 `request.js` 无重试逻辑。AI 相关接口（摘要生成、训练反馈）可能因超时失败。建议：

- 对 GET 请求增加 1 次自动重试
- 对 AI 生成类接口增加超时时间（当前统一 15s 可能不够）

---

### 18. 开发态 Dev Login 不应出现在生产包

**位置**: `pages/login/login.js` 第 13 行

```javascript
showDevLogin: config.IS_DEVELOPMENT
```

虽然通过 `IS_DEVELOPMENT` 控制了显示，但 devLogin 函数和对应 API 路径 `/api/auth/dev-login` 仍会打包进正式包。建议通过条件编译或分包完全移除。

---

### 19. 添加版本号到 app.json

当前 `app.json` 没有 `version` 信息。建议在设置页展示版本号时从 `app.json` 或 `project.config.json` 读取，而非硬编码 `"0.1.0 (MVP)"`。

---

### 20. 防重复提交增强

`target-create.js` 和 `profile.js` 使用 `submitting` 状态防重复提交，这是正确的。但 `onSubmit` 函数入口没有检查：

```javascript
onSubmit() {
  if (this.data.submitting) return; // 建议添加
  // ...
}
```

当前依赖 button 的 `loading` 状态，但快速双击可能穿透。

---

## ✅ 做得好的地方

| 项目 | 说明 |
|------|------|
| **API 路径集中管理** | `utils/api.js` 统一维护所有接口路径，杜绝 URL 散落 |
| **统一请求封装** | `utils/request.js` 自动注入 Token、401 清理、超时控制，质量很高 |
| **本地存储抽象** | `utils/storage.js` 封装存取逻辑，禁止页面直接操作 wx.setStorageSync |
| **环境自动切换** | `utils/config.js` 根据 envVersion 自动选择 API 地址 |
| **简历隐私保护** | profile 页要求用户勾选同意才可上传原文，隐私说明清晰 |
| **删除账号双确认** | settings 页两次确认 + 可选删除本地记忆，合规性好 |
| **_navTimer 清理** | 所有使用 setTimeout 导航的页面都在 onUnload 中清理了定时器 |
| **错误态展示** | 每个页面都有 loading / error / empty 三态处理 |
| **API Key password 属性** | AI Provider 表单中 API Key 输入框使用了 password 属性 |

---

## 修复优先级总结

| 优先级 | 编号 | 问题 | 工作量 |
|--------|------|------|--------|
| P0 🔴 | 1 | 主包不分包，必超 2MB | 2h |
| P0 🔴 | 2 | 隐私合规授权缺失 | 3h |
| P0 🔴 | 4 | 生产环境 API 地址为空 | 0.5h |
| P0 🔴 | 5 | 开发环境 HTTP | 1h |
| P1 🟠 | 3 | app.json 缺 permission | 0.5h |
| P1 🟠 | 6 | 自定义 Tab Bar 不完整 | 3h |
| P1 🟠 | 7 | 线性流程 redirectTo 问题 | 1h |
| P1 🟠 | 8 | 401 并发跳转 | 0.5h |
| P1 🟠 | 9 | 无分享能力 | 1h |
| P2 🟡 | 10-20 | 其他改进项 | 按需 |

**预估总修复时间**: 核心问题（P0+P1）约 12 小时，全部问题约 20 小时。

---

> 建议先修复 P0 问题再做提审，P1 问题严重影响体验也应尽快处理。P2 可以迭代优化。
