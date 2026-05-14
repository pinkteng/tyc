package com.example.demo.service;

import com.example.demo.model.dto.ChatRequestDTO;
import com.example.demo.model.vo.ChatResponseVO;

public interface ChatService {
    ChatResponseVO chat(ChatRequestDTO requestDTO);
}
