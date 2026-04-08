package com.hmdp.controller;

import com.hmdp.service.IAgentService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.hmdp.dto.Result;

@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private IAgentService agentService;

    /**
     * 同步对话：等待 Agent 完整回复后返回
     * POST /agent/chat
     * 请求体: {"message": "你好"}
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody AgentRequest request) {
        String userMessage = request.getMessage();

        // 如果前端传了位置，手动拼接到消息后面，隐式告诉 Agent 坐标
        if (request.getLocation() != null) {
            userMessage += String.format(" (当前用户位置：经度%s, 纬度%s)",
                    request.getLocation().getLongitude(),
                    request.getLocation().getLatitude());
        }

        String content = agentService.chat(userMessage);
        return Result.ok(content);
    }

    /**
     * 流式对话：SSE 逐块返回 Agent 回复
     * POST /agent/chat/stream
     * 请求体: {"message": "你好"}
     */
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody AgentRequest request) {
        return agentService.chatStream(request.getMessage());
    }

    /**
     * 请求体
     */
    public static class AgentRequest {
        private String message;

        private Location location; // 增加位置字段

        public Location getLocation() {
            return location;
        }
        public void setLocation(Location location) {
            this.location = location;
        }

        public static class Location {
            private Double longitude;

            public Double getLongitude() {
                return longitude;
            }
            public void setLongitude(Double longitude) {
                this.longitude = longitude;
            }
            private Double latitude;
            public Double getLatitude() {
                return latitude;
            }
            public void setLatitude(Double latitude) {
                this.latitude = latitude;
            }
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


}
