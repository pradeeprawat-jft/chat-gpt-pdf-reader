package com.psr.chatgptapp.service;

import com.psr.chatgptapp.dtos.ChatGptResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OpenAIService {

    String extractTextFromPdf(MultipartFile file) throws IOException;

    ChatGptResponse chat(String prompt, String bearerToken);

    ChatGptResponse callChatGptApi(MultipartFile file, String prompt) throws IOException;
}
