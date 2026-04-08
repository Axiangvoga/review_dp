package com.hmdp.service.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdp.service.IAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.service.IAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class AgentServiceImpl implements IAgentService {

    @Value("${agent.base-url:http://localhost:8000}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    //  Json字符串和Java对象转换对象
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ExecutorService agentExecutor = Executors.newFixedThreadPool(50);

    // ── 同步调用 ──────────────────────────────────────
    @Override
    public String chat(String message) {
        try {
            String body = buildBody(message);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            //  二进制数据流到String字符串转换
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Agent 调用失败: HTTP " + response.statusCode());
            }

            //  json字符“格式化”取出content部分内容去掉双引号
            return objectMapper.readTree(response.body()).path("content").asText();

        } catch (Exception e) {
            log.error("[AgentService] 同步调用失败", e);
            throw new RuntimeException("智能客服服务暂时不可用", e);
        }
    }

    // ── 流式调用（SSE）────────────────────────────────
    @Override
    public SseEmitter chatStream(String message) {

        // 建立连接管道
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        agentExecutor.submit(() -> {
            try {
                String body = buildBody(message);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/chat/stream"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMinutes(50))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                // 2. 发送请求（同步阻塞，等待返回）
                HttpResponse<java.io.InputStream> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    emitter.completeWithError(new RuntimeException("HTTP " + response.statusCode()));
                    return;
                }

                // 3. 流式读取并推送
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();

                        if ("[DONE]".equals(data)) {
                            emitter.complete();
                            return;
                        }

                        JsonNode node = objectMapper.readTree(data);
                        String chunk = node.path("content").asText("");

                        // 推送给前端 SSE
                        emitter.send(SseEmitter.event().data(chunk));
                    }
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("[AgentService] 流式调用失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String buildBody(String message) throws Exception {
        // 构造 {"message": "xxxx"}
        ObjectNode root = objectMapper.createObjectNode();

        root.put("message", message);

        String json = objectMapper.writeValueAsString(root);
        log.info("[Agent] 发送给 Python 的完整 JSON: {}", json);
        return json;
    }

}
