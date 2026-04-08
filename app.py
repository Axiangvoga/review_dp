import time
from agent.react_agent import ReactAgent
import streamlit as st

st.title("智能客服")
st.divider()

if "agent" not in st.session_state:
    st.session_state["agent"] = ReactAgent()

if "message" not in st.session_state:
    st.session_state["message"] = []

for message in st.session_state["message"]:
    st.chat_message(message["role"]).write(message["content"])

prompt = st.chat_input()

if prompt:
    st.chat_message("user").write(prompt)
    st.session_state["message"].append({"role": "user", "content": prompt})
    response = []
    with st.spinner("智能客服思考中..."):
        res_stream = st.session_state["agent"].execute_stream(prompt)


        def capture(generator, cache_list):  # 捕获

            for chunk in generator:
                cache_list.append(chunk)

                for char in chunk:
                    time.sleep(0.01)
                    yield char


        st.chat_message("assistant").write_stream(capture(res_stream, response))
        st.session_state["message"].append({"role": "assistant", "content": response[-1]})
        st.rerun()
