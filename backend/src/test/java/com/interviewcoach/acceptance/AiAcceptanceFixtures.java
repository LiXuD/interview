package com.interviewcoach.acceptance;

import java.util.List;

/**
 * Task 19: 真实 AI 验收样例集。
 * 三组典型岗位样例，每组包含目标岗位、JD、候选人摘要和样例回答，
 * 用于验证 AI 输出是否贴合岗位、候选人经历和面试能力提升目标。
 */
public class AiAcceptanceFixtures {

    // ==================== 场景 1: Java 后端/支付系统 ====================

    public static final String JAVA_TITLE = "银行统一支付平台 Java 高级后端工程师";

    public static final String JAVA_JD = """
            岗位职责：
            1. 负责银行统一支付平台核心交易链路的架构设计与开发。
            2. 支持高并发支付场景下的事务一致性、接口幂等、分布式锁等技术方案落地。
            3. 参与对账、清结算、退款等资金相关模块的设计与实现。
            4. 优化系统性能，保障 SLA 99.99% 可用性。
            5. 输出技术方案文档，参与 Code Review。

            任职要求：
            1. 5 年以上 Java 后端开发经验，精通 Spring Boot、Spring Cloud。
            2. 熟悉分布式事务（Seata/TCC）、消息队列（RocketMQ/Kafka）、缓存（Redis）。
            3. 有支付系统、金融系统开发经验优先。
            4. 熟悉 MySQL 调优、分库分表方案。
            5. 良好的沟通能力和团队协作精神。
            """;

    public static final String JAVA_SUMMARY =
            "三年 Java 后端开发经验，主导过电商支付订单模块和日终对账系统的开发。" +
            "熟悉 Spring Boot、MySQL、Redis，了解分布式事务基本概念但缺少生产落地经验。";

    public static final List<String> JAVA_SKILLS = List.of(
            "Java", "Spring Boot", "MySQL", "Redis", "RabbitMQ", "Docker"
    );

    public static final List<String> JAVA_PROJECTS = List.of(
            "电商平台支付订单模块：负责下单、支付回调、订单状态流转，日均处理 20 万笔交易",
            "日终对账系统：定时从第三方支付渠道拉取账单，与本地订单逐笔比对，生成差异报告"
    );

    public static final List<String> JAVA_EXPERIENCE = List.of(
            "3 年后端开发经验",
            "1 年支付相关业务开发"
    );

    /** 用于模拟面试和训练任务的回答样例 */
    public static final String JAVA_ANSWER_ORDER_DESIGN =
            "我在电商项目中设计了订单状态机，使用枚举定义了待支付、已支付、已发货、已完成等状态，" +
            "通过策略模式处理不同状态的流转逻辑。支付回调通过 RabbitMQ 异步处理，保证接口快速响应。" +
            "但我们在高并发场景下遇到过幂等问题，后来通过数据库唯一索引和 Redis 分布式锁解决了。";

    public static final String JAVA_ANSWER_WEAK =
            "分布式事务我了解一些，知道有 TCC 和 Saga 两种模式，" +
            "但生产环境没实际用过。我们目前用的本地事务加消息队列的方式。";

    // ==================== 场景 2: AI 应用工程师/RAG Agent ====================

    public static final String AI_TITLE = "AI 应用工程师 - RAG Agent 方向";

    public static final String AI_JD = """
            岗位职责：
            1. 设计和实现基于 RAG（Retrieval-Augmented Generation）架构的智能问答系统。
            2. 开发和优化 Prompt Engineering，提升 Agent 在垂直领域的准确率和可控性。
            3. 构建文档解析、向量化、检索增强的全链路 Pipeline。
            4. 评估和集成主流大模型（OpenAI、Claude、开源模型），实现模型路由和降级策略。
            5. 搭建评测框架，持续监控和优化 RAG 系统的 Recall 和 Faithfulness。

            任职要求：
            1. 3 年以上 Python/Java 开发经验。
            2. 熟悉 LangChain、LlamaIndex 或类似 RAG 框架。
            3. 了解 Embedding 模型、向量数据库（Milvus/Pinecone/Weaviate）。
            4. 有 Prompt Engineering 实践经验，了解 Few-shot、Chain-of-Thought 等技巧。
            5. 有 Agent 开发或多轮对话系统经验优先。
            """;

    public static final String AI_SUMMARY =
            "2 年 Python 后端开发经验，近半年在公司内部做过一个基于 LangChain 的文档问答助手原型。" +
            "熟悉 Embedding 和向量检索的基本原理，用过 Milvus 和 OpenAI API。" +
            "对 Prompt Engineering 有初步实践，但尚未在生产环境落地 RAG 系统。";

    public static final List<String> AI_SKILLS = List.of(
            "Python", "FastAPI", "LangChain", "Milvus", "OpenAI API", "Docker"
    );

    public static final List<String> AI_PROJECTS = List.of(
            "内部文档问答助手：基于 LangChain + Milvus 构建的 RAG 原型，支持上传 PDF/Markdown 后进行自然语言查询，" +
            "使用 OpenAI text-embedding-ada-002 进行向量化，检索后送入 GPT-3.5 生成回答"
    );

    public static final List<String> AI_EXPERIENCE = List.of(
            "2 年 Python 后端开发",
            "6 个月 AI 应用开发经验"
    );

    public static final String AI_ANSWER_RAG_DESIGN =
            "在我的文档问答项目中，我用 LangChain 框架搭建了完整的 RAG 流程。" +
            "文档解析用 UnstructuredLoader，切分用 RecursiveCharacterTextSplitter，" +
            "chunk size 设为 512，overlap 50。Embedding 用 OpenAI 的 ada-002 模型，" +
            "向量存储用 Milvus。检索时用 similarity_search 返回 top-5 结果作为上下文。" +
            "但我发现切分策略对问答质量影响很大，目前还在调优中。";

    public static final String AI_ANSWER_WEAK =
            "评测这块我做得比较粗浅，主要是人工看几个 case，没有系统化的评测框架。" +
            "我知道有 Recall 和 Faithfulness 指标，但没实际搭建过自动化评测 pipeline。";

    // ==================== 场景 3: 数据平台/调度数仓 ====================

    public static final String DATA_TITLE = "数据平台工程师 - 调度与数仓方向";

    public static final String DATA_JD = """
            岗位职责：
            1. 负责公司数据平台的离线调度系统设计与开发，保障数千个数据任务的稳定运行。
            2. 设计和优化数仓分层架构（ODS/DWD/DWS/ADS），提升数据复用率。
            3. 开发数据质量监控工具，实现数据异常自动发现和告警。
            4. 优化 ETL 任务性能，降低计算资源消耗。
            5. 与业务团队协作，理解数据需求并转化为技术方案。

            任职要求：
            1. 3 年以上大数据开发经验。
            2. 熟悉 Spark/Flink、Hive、Hadoop 生态。
            3. 熟悉任务调度系统（Airflow/DolphinScheduler/Azkaban）。
            4. 熟悉数仓建模理论，有分层架构实践经验。
            5. 了解数据质量治理方法论。
            """;

    public static final String DATA_SUMMARY =
            "2 年数据开发经验，主要使用 Spark SQL 和 Hive 做离线 ETL。" +
            "用过 Airflow 做任务调度，参与过数仓 ODS 到 DWD 层的开发。" +
            "对数据质量治理有概念了解，实际经验有限。";

    public static final List<String> DATA_SKILLS = List.of(
            "SQL", "Spark", "Hive", "Airflow", "Python", "Hadoop"
    );

    public static final List<String> DATA_PROJECTS = List.of(
            "电商数仓 ETL 开发：使用 Spark SQL 和 Hive 完成 ODS 到 DWD 层的数据清洗和转换，" +
            "日处理数据量约 500GB，使用 Airflow 调度 200+ 个任务",
            "数据质量监控脚本：基于 Python 开发了简单的核心指标波动检测脚本，" +
            "当关键指标偏差超过阈值时发送钉钉告警"
    );

    public static final List<String> DATA_EXPERIENCE = List.of(
            "2 年数据开发经验",
            "参与过一个中型电商数仓项目"
    );

    public static final String DATA_ANSWER_WAREHOUSE =
            "在我们的数仓项目中，我主要负责 DWD 层开发。" +
            "ODS 层是原始数据的全量快照，DWD 层做数据清洗和维度退化，" +
            "比如把订单表和用户表关联，把用户标签冗余到订单宽表中。" +
            "我们用的是 Hive 外部表 + Spark SQL，分区按天，用 Airflow 编排依赖。";

    public static final String DATA_ANSWER_WEAK =
            "数据质量治理我没有系统化实践过。" +
            "我们现在的做法比较简单，就是在关键表上写 SQL 检查空值率和波动，" +
            "但我了解 Great Expectations 这类工具，还没用过。";

    private AiAcceptanceFixtures() {}
}
