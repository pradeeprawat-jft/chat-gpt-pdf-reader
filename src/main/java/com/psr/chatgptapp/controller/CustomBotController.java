package com.psr.chatgptapp.controller;

import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.service.OpenAIService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class CustomBotController {
    private final OpenAIService service;

    public CustomBotController(OpenAIService service) {
        this.service = service;
    }


    @PostMapping("/upload")
    public String handleUpload(@RequestParam("pdfFile") MultipartFile pdfFile,
                               @RequestParam("prompt") String prompt,
                               Model model) {
        try {
            ChatGptResponse response = service.callChatGptApi(pdfFile, prompt + " form given info");

            model.addAttribute("response", response.getChoices().get(0).getMessage().getContent());
        } catch (IOException e) {
            model.addAttribute("error", "Error processing PDF file.");
        }
        return "index";
    }


    @GetMapping("/")
    public String index() {
        return "index";
    }
}

