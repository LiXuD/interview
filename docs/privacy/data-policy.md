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
| 远端教练记忆 | 远端 PostgreSQL | 中 | 结构化训练观察、用户纠错、能力短板和进步摘要 |
| 本机教练记忆归档 | iOS 本地 SwiftData 或本机文件 | 中 | 用户设备上的 `CoachingMemoryArchive`，删除账号时默认保留，可由用户选择删除 |

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
- Post-MVP 本机 `CoachingMemoryArchive` 教练记忆归档。

**远端 PostgreSQL（服务端）：**

- 确认后的 `CandidateProfile`（摘要、技能、项目、经历）。
- Post-MVP 远端 `CoachingMemory`、用户纠错记录和训练观察摘要。
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
- 删除远端 `CoachingMemory`、用户纠错记录和训练观察摘要。
- iOS 必须清空 Keychain Token 和远端同步缓存。
- 本机 `CoachingMemoryArchive` 默认保留，除非用户在删除账号页勾选“同时删除本机教练记忆文件”。

删除账号页必须明确说明：

- 云端账号、训练记录、报告、AI Provider 配置和远端教练记忆会永久删除。
- 本机教练记忆文件默认保留在当前设备上，方便用户继续复盘或后续手动导入。
- 如果用户也希望删除本机记忆，必须主动勾选删除选项。

重新登录或重新注册时：

- 如果检测到本机历史教练记忆，App 不得自动上传到新账号。
- App 必须让用户主动确认是否导入。
- 用户拒绝导入时，本机记忆不得参与后续 AI Prompt。

## 5. 教练记忆隐私边界

教练记忆只能保存结构化、可审计的信息：

- AI 实际提出的问题、题目维度、难度、考察意图和评分 rubric。
- 用户回答摘要、评分、反馈、追问、训练总结。
- 用户纠错和被用户否认的结论。
- 短板、强项、进步趋势、下一步训练重点。

教练记忆禁止保存：

- 简历原文或项目经历原文。
- API Key、Bearer Token 或 Authorization Header。
- AI hidden chain-of-thought。
- 未经用户确认的敏感原文片段。

记忆来源和可信度必须标注：

- `confirmed`：来自用户确认摘要或用户明确确认。
- `observed`：来自训练、测评、模拟面试中的实际表现。
- `corrected`：用户手动纠正过。
- `inferred`：AI 推断，只能用于追问验证，不能当事实使用。
- `rejected`：用户否认过，禁止后续再次当事实使用。

## 6. 数据隔离

- 不同用户禁止互相访问对方的 Target、Profile、Report、Provider、Agent。
- 所有业务查询必须包含用户 ID 过滤。
- iOS 本地 SwiftData 查询必须按当前用户 ID 过滤。

## 7. Agent 隐私边界

Phase 4 `InterviewCoachAgent` 隐私约束：

- Agent 数据按 `userId + targetId` 唯一隔离，所有查询必须包含用户 ID。
- Agent 不保存简历原文、用户回答原文、API Key、Authorization Header、完整请求头或 hidden chain-of-thought。
- `lastDecisionSummary` 只保存面向业务的结构化摘要，不保存模型思维链。
- Agent Observability 只记录低风险元数据：`event`、`stage`、`outcome`、`latency`；不记录 prompt、completion 或用户原文。
- Agent 随账号删除（cascade in AuthService）和目标删除（cascade in InterviewTargetService）自动清理。
- Agent 决策中的 `rationaleSummary` 可展示给用户，不包含敏感信息。
