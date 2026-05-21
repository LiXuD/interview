# AGENTS.md

@/Users/lixd/.codex/RTK.md

任何代理在修改本项目之前，必须先阅读并遵守：

`/Users/lixd/IdeaProjects/Git/interview/CLAUDE.md`

`CLAUDE.md` 是本项目 vibecoding 主约束文件。除非用户给出更新、更明确的指令，否则所有开发任务都必须以 `CLAUDE.md` 为准。

## 项目定位

本项目是 AI 技术岗面试教练 iOS App，不是题库 App，不是招聘投递工具，不是简历润色工具。

MVP 最窄闭环必须是：

```text
目标岗位 -> 简历摘要确认 -> 岗位画像 -> 5 题测评 -> 1 天训练计划 -> 1 次文字模拟面试 -> 报告
```

所有新增功能必须服务这条闭环。

## 硬约束

- 必须使用 `rtk` 前缀运行 shell 命令。
- 必须每次只实现一个小任务。
- 禁止一次实现多个 Phase。
- 禁止扩展非 MVP 功能。
- 禁止添加题库社区、招聘投递、企业端、订阅付费、多人协作、语音面试。
- 禁止实现 Anthropic 自定义 Provider；MVP 只支持平台默认 AI 和 OpenAI-compatible 自定义 Provider。

## 目录规范红线

- 新增文件必须遵守 `CLAUDE.md` 中的项目目录与模块边界规范。
- 禁止在项目根目录随意创建业务代码文件。
- 禁止把 iOS、后端、文档、脚本混放。
- 禁止绕过 feature/module 边界创建临时目录。

## 后端安全红线

- 不得跳过 Spring Security。
- 必须使用 Spring Security Bearer Token 拦截器链。
- 业务接口必须从 `SecurityContextHolder` 获取当前用户。
- 禁止在 Controller 或 Service 中硬编码用户 ID。
- 禁止不同用户访问彼此的数据。

## 隐私红线

- 简历原文默认只允许保存在 iOS 本地。
- 生成摘要时，简历原文只允许临时上传并在后端内存中使用。
- 简历原文禁止落 PostgreSQL、Redis、文件、缓存、审计表。
- 禁止记录简历原文到 `System.out`、`System.err` 或任何日志框架。
- 禁止在日志中输出 `resumeText`、`rawResume`、`projectRawText`、`originalText` 或任何原文字段。
- API Key 必须加密保存，禁止返回给 iOS，禁止写入日志。

## API 与 AI 输出红线

- 所有后端返回给 iOS 的 JSON 必须使用 camelCase。
- 后端必须返回强类型 DTO。
- 禁止把 AI 原始字符串直接返回给 iOS。
- 禁止让 iOS 解析 AI 原始文本。
- AI 结构化输出必须在后端解析为 DTO。
- AI 解析失败必须返回统一错误响应，禁止返回半成品字符串。

## MockInterview 红线

- 后端组装模拟面试 Prompt 时，最多携带最近 6 轮，也就是 12 条 message。
- 禁止把完整模拟面试历史塞给模型。
- 禁止 Prompt 随对话轮次无限增长。

## Report 生命周期

- `AssessmentSession` finish 创建 `Report(type=assessment)`。
- `MockInterview` finish 创建 `Report(type=mockInterview)`。
- `TrainingTask` answer 只生成 `TrainingFeedback`，不创建 Report。

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
