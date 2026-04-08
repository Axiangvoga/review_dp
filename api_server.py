# api_server.py
# pip install fastapi uvicorn
# 启动: uvicorn api_server:app --host 0.0.0.0 --port 8000
# uvicorn api_server:app --host 0.0.0.0 --port 8000 --reload

import json
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from agent.react_agent import ReactAgent

app = FastAPI()
agent = ReactAgent()


class ChatRequest(BaseModel):
    message: str


# ── 同步接口 ──────────────────────────────────────────
@app.post("/api/chat")
def chat(req: ChatRequest):
    chunks = list(agent.execute_stream(req.message))
    return {"content": chunks[-1]}


# ── 流式接口（SSE）────────────────────────────────────
@app.post("/api/chat/stream")
def chat_stream(req: ChatRequest):
    def generate():
        for chunk in agent.execute_stream(req.message):
            yield f"data: {json.dumps({'content': chunk}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})