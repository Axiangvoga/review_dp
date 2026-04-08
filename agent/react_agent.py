from langchain.agents import create_agent
from model.factory import chat_model
from utils.prompt_loader import load_system_prompts
from agent.tools.middleware import log_before_model, monitor_tool, report_prompt_switch
from agent.tools.agent_tools import (rag_summarize, get_weather, get_user_id,get_user_location,
                                           get_current_month, fetch_external_data, fill_context_for_report,get_nearby_shops)


class ReactAgent:
    def __init__(self):
        self.agent = create_agent(
            model=chat_model,
            system_prompt=load_system_prompts(),
            middleware=[log_before_model, monitor_tool, report_prompt_switch],
            tools=[rag_summarize, get_weather, get_user_id,get_user_location,
                   get_current_month, fetch_external_data, fill_context_for_report, get_nearby_shops]
        )

    def execute_stream(self, query):
        input_dict = {
            "messages": [
                {"role": "user", "content": query}
            ]
        }

        for chunk in self.agent.stream(input_dict, stream_mode="values", context={"report": False}):
            latest_message = chunk["messages"][-1]
            if latest_message.content:
                yield latest_message.content.strip() + "\n"


if __name__ == '__main__':
    agent = ReactAgent()

    for chunk in agent.execute_stream("给我生成我的使用报告"):
        print(chunk, end="", flush=True)
