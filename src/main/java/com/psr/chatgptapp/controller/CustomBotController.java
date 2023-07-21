package com.psr.chatgptapp.controller;

import com.psr.chatgptapp.config.GmailApiClient;
import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.helper.EmailUtils;
import com.psr.chatgptapp.service.OpenAIService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Controller
public class CustomBotController {
    private final OpenAIService service;
    private final GmailApiClient gmailApiClient;

    public CustomBotController(OpenAIService service, GmailApiClient gmailApiClient) {
        this.service = service;
        this.gmailApiClient = gmailApiClient;
    }


    @PostMapping("/upload")
    public String handleUpload(@RequestParam("pdfFile") MultipartFile pdfFile,
                               @RequestParam("prompt") String prompt,
                               @RequestParam(value = "password", required = false) String password,
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
    public String home() throws Exception {
        System.out.println("here ");
        return "redirect:/gmail/authorize";

    }

    @GetMapping("/gmail/authorize")
    public String authorize() throws Exception {
        String authorizationUrl = gmailApiClient.getAuthorizationUrl();
        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/gmail/oauth/callback")
    public String callback(@RequestParam("code") String code) throws Exception {
        gmailApiClient.authorize(code);
        return "redirect:/gmail/emails";
    }

    @GetMapping("/gmail/emails")
    public String listEmails(Model model) throws IOException, GeneralSecurityException {
        List<Map<String, String>> emails = EmailUtils.parseMessages(gmailApiClient.getGmail(), gmailApiClient.listEmails());
        model.addAttribute("emails", emails);
        return "index";
    }

}

