# 数据隐私策略

本文档定义 InterviewCoach App 中敏感数据的存储、传输和处理边界。所有开发者和 AI 代理必须遵守本文档。

## 1. 数据分类

| 数据类型 | 存储位置 | 敏感级别 | 说明 |
|----------|----------|----------|------|
| 简历原文 | iOS 本地 SwiftData | 高 | 默认只存本地，永不落远端库 |
| 候选人摘要 | 远端 PostgreSQL | 中 | 用户确认后的结构化摘要 |
| Bearer Token | iOS Keychain | 高 | 登录凭证 |
| API Key | 远端 PostgreSQL（加密） | 高 | 用户自定义 AI Provider 密钥 |
| 岗位目标 | 远端 PostgreSQL + iOS 本地 | 低 | 非敏感业务数据 |

## 2. 简历摘要隐私链路

### 2.1 流程

1. 用户在 iOS 粘贴简历或项目经历，原文只存本地 SwiftData。
2. 用户点击"生成摘要"前，App 必须明确提示：原文将临时发送到后端 AI 进行摘要生成，不会落库。
3. 后端 `POST /api/profiles/draft-summary` 接收原文。
4. 后端只允许在内存中使用原文调用 AI。
5. 后端不得保存原文，不得记录原文日志。
6. 后端返回 `CandidateProfileDraftDto`。
7. 用户在 iOS 编辑并确认摘要。
8. iOS 调用 `POST /api/profiles/confirm`。
9. 后端只保存确认后的 `CandidateProfile` 摘要。

### 2.2 存储边界

**iOS 本地 SwiftData（纯本地）：**

- 简历原文。
- 用户未确认上传的项目经历原文。
- 已确认的候选人摘要（离线缓存）。

**远端 PostgreSQL（服务端）：**

- 确认后的 `CandidateProfile`（摘要、技能、项目、经历）。
- 不包含任何简历原文字段。

**远端数据库禁止存储：**

- `resumeText`
- `projectRawText`
- 原文的任何片段或哈希

### 2.3 日志约束

在 `profiles/draft-summary` 相关代码中（Controller、Service、AI adapter），以下行为**严格禁止**：

- 将简历原文输出到 `System.out`、`System.err` 或任何日志框架。
- 使用 `logger.info`、`logger.debug`、`logger.warn`、`logger.error` 记录 `resumeText`、`rawResume`、`projectRawText`、`originalText` 或任何原文字段。

**允许记录：**

- `requestId`
- `userId`
- 原文字符长度（`rawTextLength`）
- 处理耗时

### 2.4 Code Review 检查项

完成 CandidateProfile 相关任务前，必须搜索以下模式，确认没有违规：

```bash
# 搜索 Controller、Service、AI adapter 中的原文日志
grep -r "resumeText\|rawResume\|projectRawText\|originalText" backend/src/main/java/com/interviewcoach/profile/
grep -r "resumeText\|rawResume\|projectRawText\|originalText" backend/src/main/java/com/interviewcoach/ai/
grep -r "System.out\|System.err\|logger\." backend/src/main/java/com/interviewcoach/profile/controller/
grep -r "System.out\|System.err\|logger\." backend/src/main/java/com/interviewcoach/profile/service/
```

任何违规必须在提交前修复。

## 3. API Key 隐私

- 用户自定义 AI Provider 的 API Key 必须后端加密保存。
- API Key 禁止返回给 iOS（不在任何 DTO 中）。
- API Key 禁止写入日志。
- Authorization Header 禁止写入日志。
- 删除 Provider 时必须同时删除密钥。

## 4. 用户删除账号

`DELETE /api/me` 必须：

- 删除远端 PostgreSQL 中该用户的所有数据（Target、Profile、Assessment、Training、MockInterview、Report、Provider）。
- iOS 必须清空本地 SwiftData 和 Keychain Token。

## 5. 数据隔离

- 不同用户禁止互相访问对方的 Target、Profile、Report、Provider。
- 所有业务查询必须包含用户 ID 过滤。
- iOS 本地 SwiftData 查询必须按当前用户 ID 过滤。
