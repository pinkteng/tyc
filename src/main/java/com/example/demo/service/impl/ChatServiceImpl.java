package com.example.demo.service.impl;

import com.example.demo.model.dto.ChatRequestDTO;
import com.example.demo.model.vo.ChatResponseVO;
import com.example.demo.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatClient chatClient;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
            StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一名专业、友好、简洁的中文智能助手，结合历史对话回答用户问题。")
                .defaultOptions(DashScopeChatOptions.builder()
                        .withTopP(0.7)
                        .build())
                .build();
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        String sessionId = requestDTO.getSessionId();
        String message = requestDTO.getMessage();
        String redisKey = "chat:session:" + sessionId;

        // 1. 读取历史消息
        List<String> records = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
        String historyText = "";
        if (records != null && !records.isEmpty()) {
            historyText = String.join("\n", records);
        }

        // 2. 拼接上下文
        String finalPrompt = """
                以下是历史对话：
                %s
                当前用户问题：
                %s
                """.formatted(historyText, message);

        // 3. 调用大模型
        String answer = chatClient.prompt(finalPrompt).call().content();

        // 4. 保存本轮记录
        String recordText = "用户：" + message + "\n助手：" + answer;
        stringRedisTemplate.opsForList().rightPush(redisKey, recordText);

        // 5. 只保留最近 3 轮
        Long size = stringRedisTemplate.opsForList().size(redisKey);
        if (size != null && size > 3) {
            stringRedisTemplate.opsForList().trim(redisKey, size - 3, size - 1);
        }

        return new ChatResponseVO(message, answer);
    }
}
