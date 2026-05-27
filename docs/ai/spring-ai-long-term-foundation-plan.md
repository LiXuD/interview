# Spring AI 长期底座迁移方案

## 1. 背景

当前后端 AI 模块已经完成平台默认真实 AI、OpenAI-compatible 自定义 Provider、AI 结构化输出解析、真实 AI 门禁和 Task 19 live AI 验收样例集。现阶段 Task 18-25 仍由其他团队推进，本方案不插队改造当前任务，不改变正在开发的业务范围。

本方案用于当前开发任务全部完成后，将 AI 调用底座逐步迁移到 Spring AI。目标不是追求框架替换本身，而是降低长期维护成本，提升结构化输出、超时控制、观测、记忆和自适应教练能力的工程稳定性。

## 2. 迁移目标

- 保留后端统一代理 AI 的边界，iOS 继续只调用后端业务 API。
- 保留平台默认 AI 和用户 OpenAI-compatible 自定义 Provider。
- 保留 API Key 加密保存、禁止返回 iOS、禁止写日志的安全约束。
- 保留 `AiStructuredOutputService` 作为强类型 DTO 校验边界。
- 引入 Spring AI 的 `ChatClient`、OpenAI-compatible ChatModel、结构化输出、Advisor、Chat Memory 和 Observability 能力。
- 让 live AI 验收在 provider 慢、网络不可达、模型输出异常时能在固定时间内明确失败，并定位到具体 AI task。

## 3. 非目标

- 不在当前 Task 18-25 进行中途重构。
- 不引入 Anthropic 自定义 Provider。
- 不让 iOS 直接调用 Spring AI 或任何大模型。
- 不保存 AI hidden chain-of-thought。
- 不把 Spring AI Chat Memory 直接当作业务教练记忆事实来源。
- 不一次性替换所有 AI task；迁移必须按小任务推进。

## 4. 当前实现问题

当前 AI 模块的优点是边界清晰：`PlatformAiClient` 负责平台调用，`OpenAiCompatibleClient` 负责 OpenAI-compatible HTTP 请求，`AiStructuredOutputService` 负责 JSON 解析和 DTO 校验，业务 service 负责 ID 回填和持久化。

主要长期问题：

- `RestTemplate` 当前没有 connect/read timeout，live AI 验收在 provider 慢或不可达时可能长时间挂起。
- OpenAI-compatible JSON mode、responses mode、错误映射、重试、观测都由项目手写维护。
- 随着 Task 20-25 引入结构化测评、逐题诊断、教练记忆和自适应追问，自研 prompt/JSON 解析代码会继续膨胀。
- 缺少统一的 token、latency、model、task 维度观测。

## 5. 为什么选择 Spring AI

Spring AI 的价值主要体现在以下方面：

- `ChatClient` 支持把模型输出映射为 Java entity，适合本项目“AI 原始响应只能停留在后端，返回 iOS 前必须是强类型 DTO”的约束。
- Spring AI OpenAI Chat 支持 `JSON_OBJECT` 和 `JSON_SCHEMA` response format，可以逐步从“合法 JSON”升级为“按 schema 输出”。
- Advisor API 适合承载 prompt 增强、task 标签、观测上下文、记忆摘要注入等横切逻辑。
- Chat Memory 提供 message window 和持久化 repository 抽象，可作为自适应模拟面试的上下文窗口工具，但不能替代业务 `CoachingMemory`。
- Observability 支持 Spring 生态内的 metrics/tracing，可对模型调用、token usage、延迟等进行统一观测。

官方参考：

- Spring AI ChatClient API: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring AI OpenAI Chat: https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html
- Spring AI Advisors: https://docs.spring.io/spring-ai/reference/api/advisors.html
- Spring AI Chat Memory: https://docs.spring.io/spring-ai/reference/api/chat-memory.html
- Spring AI Observability: https://docs.spring.io/spring-ai/reference/observability/index.html

## 6. 目标架构

迁移后保持三层边界：

```text
业务模块
  -> AiStructuredOutputService
      -> AiModelGateway
          -> SpringAiPlatformClient
          -> SpringAiUserProviderClient
          -> LocalPlatformAiClient（仅测试/离线演示）
```

### 6.1 AiStructuredOutputService

继续作为唯一结构化输出入口。职责：

- 根据 `AiPrompt.task` 选择 DTO 类型。
- 调用 `AiModelGateway` 获得模型输出。
- 解析为 DTO。
- 运行业务校验，例如数组非 null、分数范围、枚举合法、字段非 blank。
- 对不可信 ID 字段采用后端回填或严格匹配策略。
- AI 解析失败统一抛出 `AiParseException`。

### 6.2 AiModelGateway

新增内部网关接口，替代直接依赖 `PlatformAiClient` 和 `OpenAiCompatibleClient` 的散落调用。

建议接口：

```java
public interface AiModelGateway {
    String generateJson(AiPrompt prompt);
}
```

第一阶段保持返回 String，降低迁移风险。结构化 DTO 映射稳定后，再评估增加：

```java
<T> T generateEntity(AiPrompt prompt, Class<T> responseType);
```

### 6.3 SpringAiPlatformClient

用于平台默认 AI。配置来源必须仍然是：

- `IC_PLATFORM_AI_ENABLED`
- `IC_PLATFORM_AI_BASE_URL`
- `IC_PLATFORM_AI_API_KEY`
- `IC_PLATFORM_AI_MODEL`
- `IC_PLATFORM_AI_MODE`
- `IC_REQUIRE_REAL_AI_FOR_COACHING`

平台 API Key 不允许写入代码、日志或测试输出。

### 6.4 SpringAiUserProviderClient

用于用户自定义 OpenAI-compatible Provider。要求：

- 用户 Provider 优先于平台 Provider。
- API Key 从数据库密文读取后只在请求内存中解密使用。
- Provider 调用失败时不得自动切换平台 AI。
- 不在单例 Bean 中长期保存用户 API Key。
- 可以按 provider 配置构造短生命周期 ChatModel，或实现受控缓存；缓存键不得包含明文 API Key。

### 6.5 LocalPlatformAiClient

继续保留，但只能用于：

- 单元测试。
- CI 非 live AI 回归。
- 明确标记的离线演示。
- 基础健康检查。

核心教练路径在 `stubOnly` 状态下仍必须阻止继续。

## 7. 结构化输出策略

分两步迁移：

第一步：Spring AI 只替代 HTTP 调用，输出仍为 JSON String，由 `AiStructuredOutputService` 使用 Jackson 解析并校验。

第二步：对稳定 DTO 使用 Spring AI structured output：

- 简单 DTO 使用 `ChatClient.call().entity(ResponseDto.class)`。
- 对 Task 20 之后的复杂结构化测评，优先使用 `JSON_SCHEMA`。
- JSON Schema 必须来自后端 DTO 契约，不允许让模型自由扩展字段。
- 即使 Spring AI 已解析成 DTO，后端仍必须保留二次业务校验。

ID 策略：

- `candidateProfileDraft`：不信任 AI 的 `rawTextLength`，后端计算。
- `jobBrief`：返回给 iOS 的 `targetId` 使用后端 target ID。
- `assessmentResult`：返回给 iOS 的 `assessmentId` 使用后端 session ID。
- `trainingFeedback`：返回给 iOS 的 `taskId` 使用后端 task ID。
- `mockInterviewReport`：返回给 iOS 的 `mockInterviewId` 使用后端 interview ID。

## 8. 超时、重试与错误映射

迁移前必须先定义统一调用策略：

- connect timeout：建议 5 秒。
- read timeout：建议 60 秒。
- live AI 验收单个 task 最大耗时：建议 90 秒。
- 默认不自动重试完整业务生成，避免重复扣费和不可预期内容漂移。
- 仅对明确可重试的网络瞬断做 1 次受控重试。
- 用户 Provider 失败返回 `AiProviderCallFailedException`，不得自动切换平台 Provider。
- AI 输出解析失败返回 `AI_PARSE_FAILED`，不得返回半成品字符串。

错误响应必须包含：

- task name。
- provider 类型：`userProvider` 或 `platformProvider`。
- model。
- requestId。
- 错误类别：配置缺失、网络超时、HTTP 非 2xx、解析失败、内容校验失败。

错误响应不得包含：

- API Key。
- Authorization Header。
- 完整请求头。
- 简历原文。
- AI 原始响应全文。

## 9. Observability 策略

启用 Spring AI observability 时，默认不得采集 prompt/completion 内容。只允许采集低风险元数据：

- task name。
- provider 类型。
- model。
- latency。
- token usage。
- success/failure。
- error category。
- requestId。

禁止默认采集：

- JD 全文。
- 简历原文。
- 用户回答全文。
- prompt 完整内容。
- completion 完整内容。
- API Key 或请求头。

如需调试 prompt，必须使用本地开发开关，并确保日志脱敏、默认关闭、不得提交真实数据。

## 10. Chat Memory 与教练记忆边界

Spring AI Chat Memory 可以用于管理短窗口对话上下文，但业务教练记忆仍必须由项目自己的 DTO、表和校验规则承载。

使用边界：

- 模拟面试上下文继续遵守最近 6 轮、最多 12 条 message。
- Chat Memory 只作为上下文装配工具，不作为事实库。
- 远端 `CoachingMemory` 必须区分 `confirmed`、`observed`、`corrected`、`inferred`、`rejected`。
- `inferred` 只能用于追问验证，不能当事实。
- `rejected` 禁止再次作为事实。
- 删除账号时远端记忆必须删除。

## 11. 分阶段迁移计划

### Phase 0: 当前任务收尾后的冻结点

进入 Spring AI 迁移前必须满足：

- Task 18-25 已完成并通过默认回归。
- live AI 验收可以跑通或在固定时间内明确失败。
- `docs/ai/prompt-contracts.md`、`docs/ai/provider-contracts.md` 与实现一致。
- MockInterview prompt 仍限制最近 6 轮、最多 12 条 message。

### Phase 1: AI HTTP 调用硬化

目标：不引入 Spring AI，先修复当前 AI 调用的可靠性基础。

范围：

- 为当前 `RestTemplate` 配置 connect/read timeout。
- 为 live AI 测试增加 task 级耗时上限。
- 为 provider 调用失败补充 task/model/provider 错误分类。

验收：

- provider 不可达时，测试在固定时间内失败。
- 错误不包含 API Key、Authorization Header 或原始 prompt。
- 默认 `rtk mvn -q test` 不依赖 live AI。

### Phase 2: 引入 Spring AI 依赖但不改业务行为

目标：加入 Spring AI 依赖和最小配置，保证编译和测试稳定。

范围：

- 在 `backend/pom.xml` 引入 Spring AI BOM 和 OpenAI starter。
- 新增 Spring AI 配置类，但不替换生产路径。
- 增加配置绑定测试，确认环境变量仍沿用 `IC_PLATFORM_AI_*`。

验收：

- 默认测试通过。
- 未配置 Spring AI 时不会影响现有 AI 路径。
- 不引入 Anthropic Provider 业务入口。

### Phase 3: 平台 Provider 迁移到 Spring AI

目标：平台默认 AI 走 Spring AI，用户自定义 Provider 暂时保持旧实现。

范围：

- 新增 `SpringAiPlatformClient`。
- 在平台 AI enabled 且配置完整时使用 Spring AI。
- 保留 `PlatformRealAiClient` 作为回退开关，便于灰度。

验收：

- 平台真实 AI 可通过 live AI 验收。
- stub 状态不会被误展示为真实 AI。
- 平台配置缺失时明确失败。

### Phase 4: 用户 OpenAI-compatible Provider 迁移

目标：用户 Provider 也通过 Spring AI 统一调用。

范围：

- 新增 `SpringAiUserProviderClient`。
- 支持用户配置的 baseUrl、model、mode。
- 保留 API Key 解密只在请求内存中发生。
- Provider 失败时不得自动切换平台 AI。

验收：

- 用户默认 Provider 优先。
- 删除 Provider 后密钥不可再用。
- API Key 不出现在响应、日志、异常 message 中。

### Phase 5: 结构化输出升级

目标：稳定 task 使用 Spring AI structured output，复杂 task 使用 JSON Schema。

迁移顺序：

1. `candidateProfileDraft`
2. `jobBrief`
3. `assessmentQuestions`
4. `assessmentResult`
5. `trainingPlan`
6. `trainingFeedback`
7. `mockInterviewQuestion`
8. `mockInterviewReport`

验收：

- 每个 task 都有合法输出测试、非法 JSON 测试、缺失字段测试。
- 包含 ID 的 task 覆盖 AI 返回错误 ID 的场景。
- DTO 二次业务校验仍保留。

### Phase 6: Advisor 与记忆增强

目标：在不破坏业务记忆约束的前提下，引入 Advisor 管理 prompt 装配和短窗口上下文。

范围：

- 为 task name、provider、model、requestId 增加 Advisor context。
- 模拟面试使用受控 message window。
- 教练记忆只注入必要摘要，不注入完整历史。

验收：

- Prompt 不随轮次无限增长。
- 仍然最多携带最近 6 轮、12 条 message。
- `rejected` 记忆不会再次作为事实进入 prompt。

## 12. 测试矩阵

| 测试类型 | 是否默认运行 | 目的 |
| --- | --- | --- |
| 单元测试 | 是 | DTO 校验、错误映射、ID 回填 |
| Controller 测试 | 是 | Bearer Token、安全隔离、API camelCase |
| 非 live AI 回归 | 是 | 使用 stub 验证业务闭环 |
| live AI 验收 | 否，显式开启 | 验证真实模型质量和结构化输出 |
| Provider 失败测试 | 是 | 验证不自动降级、不泄露密钥 |
| 超时测试 | 是 | 验证 provider 慢时固定时间失败 |

## 13. 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| Spring AI 版本变化影响 API | 使用独立 adapter 层，不让业务 service 直接依赖 Spring AI |
| OpenAI-compatible Provider 兼容性差异 | 保留旧客户端灰度开关，live AI 样例覆盖平台和用户 Provider |
| JSON Schema 对部分模型支持不一致 | 先保持 JSON String + Jackson 校验，再逐步启用 schema |
| Observability 暴露敏感 prompt | 默认关闭 prompt/completion 内容采集，只采集元数据 |
| Chat Memory 与业务记忆混用 | 明确 Chat Memory 只做短窗口上下文，事实记忆仍用业务 DTO |
| 迁移影响正在开发任务 | 只在 Task 18-25 完成后启动，按 Phase 小步提交 |

## 14. 推荐决策

推荐采用“适配层渐进迁移”。

不要把 Spring AI 直接灌进业务 service，也不要一次性替换 `AiStructuredOutputService`。先让 Spring AI 成为底层模型调用和观测底座，再逐步迁移结构化输出和 Advisor。这样可以保留现有隐私、安全、Provider 优先级和 AI 质量验收约束，同时为后续自适应教练能力提供更稳的工程基础。

