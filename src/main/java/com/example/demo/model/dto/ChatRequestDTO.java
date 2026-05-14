package com.example.demo.model.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    /** 会话编号，用于标识同一轮连续对话 */
    private String sessionId;
    /** 当前用户输入 */
    private String message;
}
