# AI Provider 规约

本文档定义 InterviewCoach App 中 AI Provider 的类型、配置、调用和安全规则。所有开发者和 AI 代理必须遵守本文档。

## 1. Provider 类型

MVP 支持两种 Provider：

| Provider | `type` | 说明 |
|----------|--------|------|
| 平台默认 AI | `platformDefault` | 后端内置，无需用户配置 |
| 用户自定义 OpenAI-compatible | `userOpenAICompatible` | 用户提供 baseUrl、apiKey、model、openaiApiMode |

MVP 禁止实现 Anthropic Provider。Anthropic 只允许在架构中保留扩展点。

## 2. 平台默认 AI

MVP 阶段平台默认 AI 由 `LocalPlatformAiClient` 提供本地 stub，返回稳定的结构化 JSON，保证无外部密钥时仍可完成演示闭环。

Post-MVP AI 质量阶段将平台默认 AI 升级为可配置的 OpenAI-compatible 后端模型能力：

- 平台真实 AI 启用时，后端使用平台配置调用 OpenAI-compatible API。
- 平台真实 AI 未启用时，`LocalPlatformAiClient` 只能作为单元测试、CI 非 live AI 回归、明确标记的离线演示和基础健康检查兜底。
- 用户已配置默认 Provider 时，用户 Provider 优先于平台默认 AI。
- 平台真实 AI 启用但配置缺失时必须明确失败，不允许静默回退到本地 stub。
- Task 18-25 阶段，开发环境也必须能连接真实 AI；测评、训练、模拟面试等核心教练路径禁止静默使用 stub。

### 2.1 平台默认 AI 配置

平台真实 AI 配置只能来自环境变量或部署配置，禁止把平台 API Key 写入仓库。

| 环境变量 | 必填条件 | 说明 |
|----------|----------|------|
| `IC_PLATFORM_AI_ENABLED` | 是 | 是否启用平台真实 AI，建议取值 `true` 或 `false` |
| `IC_PLATFORM_AI_BASE_URL` | 启用时必填 | OpenAI-compatible API 基础 URL |
| `IC_PLATFORM_AI_API_KEY` | 启用时必填 | 平台默认 AI API Key |
| `IC_PLATFORM_AI_MODEL` | 启用时必填 | 平台默认模型名称 |
| `IC_PLATFORM_AI_MODE` | 启用时必填 | `chatCompletions` 或 `responses` |

安全要求：

- `IC_PLATFORM_AI_API_KEY` 不得返回给 iOS。
- 禁止将平台 API Key、Authorization Header 或完整请求头写入日志。
- 平台默认 AI 仍必须通过后端统一代理，iOS 禁止直接调用 AI。

## 3. 用户自定义 OpenAI-compatible Provider

### 3.1 配置字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 用户自定义名称 |
| `baseUrl` | String | 是 | OpenAI-compatible API 基础 URL |
| `apiKey` | String | 是 | API 密钥（仅创建时传入，不返回 iOS） |
| `model` | String | 是 | 模型名称 |
| `openaiApiMode` | String | 是 | `chatCompletions` 或 `responses` |

### 3.2 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai-providers` | 列出当前用户所有 Provider |
| POST | `/api/ai-providers` | 创建 Provider |
| POST | `/api/ai-providers/test` | 测试连接（不保存） |
| POST | `/api/ai-providers/models` | 通过后端临时请求 Provider 的 `GET {baseUrl}/models`，返回可选模型 ID（不保存） |
| PATCH | `/api/ai-providers/{id}/default` | 设为默认 Provider |
| DELETE | `/api/ai-providers/{id}` | 删除 Provider |

### 3.3 `openaiApiMode`

- `chatCompletions`：调用 `POST {baseUrl}/chat/completions`，使用 messages 数组。
- `responses`：调用 `POST {baseUrl}/responses`，使用 input 字段。
- 模型列表：调用 `GET {baseUrl}/models`，解析 OpenAI-compatible 响应中的 `data[].id`。该接口用于创建 Provider 时辅助选择模型；若 Provider 不支持或返回空列表，iOS 必须允许用户手动输入 `model`。

## 4. Provider 路由

`AiStructuredOutputService.generateFromProvider()` 中的路由逻辑：

1. 通过 `SecurityContextHolder` 获取当前用户。
2. 调用 `AiProviderService.findDefaultProvider(userId)` 查询默认 Provider。
3. 若存在默认 Provider → 解密 apiKey → 调用 `OpenAiCompatibleClient.generateJson()`。
4. 若不存在 → 调用 `PlatformAiClient.generateJson()`（平台默认 AI）。
5. 平台真实 AI 未启用时 → 由 `LocalPlatformAiClient` 返回本地 stub JSON。

**禁止自动回退**：当用户 Provider 调用失败时，不自动切换到平台 AI，必须让用户确认。

### 4.1 真实教练模式门禁

Post-MVP Real AI Adaptive Coaching 阶段必须区分 AI 运行状态：

| 状态 | 含义 | 核心教练路径 |
|------|------|--------------|
| `realUserProvider` | 当前用户配置了默认 OpenAI-compatible Provider | 允许 |
| `realPlatformProvider` | 平台真实 AI 已启用且配置完整 | 允许 |
| `stubOnly` | 仅有 `LocalPlatformAiClient` stub | 阻止 |
| `unavailable` | 没有可用 Provider 或配置错误 | 阻止 |

核心教练路径包括：测评出题、测评评分、训练反馈、专项训练、模拟面试追问和报告复盘。`stubOnly` 只能用于测试、CI 非 live AI 回归、离线演示和基础健康检查，不能让开发者误以为真实 AI 能力已达标。

## 5. 安全要求

| 规则 | 说明 |
|------|------|
| API Key 加密存储 | AES-GCM 加密，密钥通过 `app.ai.encryption-key` 配置注入 |
| API Key 不返回 iOS | `AiProviderDto` 中不含 `apiKey` 字段 |
| API Key 不写日志 | 禁止 logger 输出 apiKey 或 apiKeyEncrypted |
| Authorization Header 不写日志 | 禁止 logger 输出 Authorization header |
| 删除时删除密钥 | `DELETE /api/ai-providers/{id}` 同时删除 encrypted key |
| 级联删除 | `DELETE /api/me` 时通过 `aiProviderRepository.deleteByUserId(userId)` 删除 |

## 6. 加密实现

`ApiKeyEncryption` 使用 AES/GCM/NoPadding：

- IV：随机 12 字节，每次加密不同。
- Tag：128 bit。
- 密钥：32 字节（AES-256），从 Base64 编码的配置读取。
- 存储格式：Base64(IV + ciphertext)。

### 开发环境密钥

```yaml
# application.yml
app.ai.encryption-key: ZGV2LWFpLWVuY3J5cHRpb24ta2V5LTMyLWJ5dGVzISE=
```

### 测试环境密钥

```yaml
# application-test.yml
app.ai.encryption-key: dGVzdC1haS1lbmNyeXB0aW9uLWtleS0zMmJ5dGVzISE=
```

## 7. 与业务模块的集成

业务模块（JobBrief、Assessment、Training、MockInterview）不直接依赖 `AiProviderService`。所有 AI 调用通过 `AiStructuredOutputService` 统一封装，Provider 选择对业务模块透明。
