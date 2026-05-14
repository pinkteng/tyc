package com.example.demo.controller;

import com.example.demo.model.dto.ChatRequestDTO;
import com.example.demo.model.vo.ChatResponseVO;
import com.example.demo.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponseVO chat(@RequestBody ChatRequestDTO requestDTO) {
        return chatService.chat(requestDTO);
    }
}
