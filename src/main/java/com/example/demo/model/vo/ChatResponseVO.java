package com.example.demo.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponseVO {
    private String userMessage;
    private String assistantMessage;
}
