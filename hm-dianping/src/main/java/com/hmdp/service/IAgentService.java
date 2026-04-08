package com.hmdp.service;


import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAgentService {
    /** 同步调用，返回完整回复 */
    String chat(String message);

    /** 流式调用，返回 SSE */
    SseEmitter chatStream(String message);
}
