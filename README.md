🤖 智能导购 Agent (LLM-Driven Intelligent Assistant)
本项目基于 LangChain / Spring AI 框架，接入 通义千问 (Qwen) 大模型，为原有的本地生活平台注入了“大脑”。它打破了传统“搜索 -> 点击 -> 预订”的繁琐流程，实现了基于自然语言驱动的一站式闭环服务。

核心能力 (Core AI Capabilities)
1. 语义推荐与主动唤起
意图识别：能够精准识别用户模糊的消费意图（如：“帮我找找附近环境好且适合约会的餐厅”），并自动匹配后端的 GEO 检索 和 商户分类查询。

多轮对话上下文：维持对话状态，支持用户基于前文进行追问（如：“那家店现在还有秒杀券吗？”）。

2. 工具自动化封装 (Function Calling / Tool Use)
将复杂的后端业务逻辑封装为 Agent 可调用的工具（Tools）：

GEO 搜索工具：Agent 自动提取用户位置经纬度，调用 Redis GEO 指令查询周边商户。

优惠券分析工具：实时查询 Redis 缓存中的秒杀券库存与逻辑过期状态。

自动预订/下单工具：在获取用户确认后，Agent 自动填充参数并调用下单接口，实现业务闭环。

3. 高性能与鲁棒性优化
流式输出 (Stream)：基于 Server-Sent Events (SSE) 技术，实现大模型回答的实时流式渲染，显著降低用户感知的首字延迟。

指数退避重试 (Exponential Backoff)：针对第三方 LLM API 可能出现的限流（HTTP 429）或网络抖动，封装了高可靠的重试机制，确保复杂任务流不中断。

提示词工程 (Prompt Engineering)：精心设计的 System Prompt，确保 Agent 在处理商户推荐时保持客观、结构化，并严格遵循业务字段限制。
