package com.psr.chatgptapp.controller;

import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.service.OpenAIService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class CustomBotController {
    private final OpenAIService service;

    public CustomBotController(OpenAIService service) {
        this.service = service;
    }


    @PostMapping("/upload")
    public String handleUpload(@RequestParam("pdfFile") MultipartFile pdfFile,
                               @RequestParam("prompt") String prompt,
                               @RequestParam(value = "password",required = false) String password,
                               Model model) {
        try {
            if (pdfFile.isEmpty()) {
                model.addAttribute("error", "pdf Should be there!!");
                return "index";
            }
            boolean status = service.savePdf(pdfFile);
            if (status) {
                ChatGptResponse response = service.callChatGptApi(pdfFile, prompt + " form given info", password);
                model.addAttribute("response", response.getChoices().get(0).getMessage().getContent());
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "index";
    }


    @GetMapping("/")
    public String index() {
        return "index";
    }
}

