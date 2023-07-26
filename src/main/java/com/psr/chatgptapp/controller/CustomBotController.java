package com.psr.chatgptapp.controller;

import com.psr.chatgptapp.config.GmailApiClient;
import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.helper.EmailUtils;
import com.psr.chatgptapp.service.OpenAIService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Controller
public class CustomBotController {
    private final OpenAIService service;
    private final GmailApiClient gmailApiClient;
    private String extractText;
    private String modifiedFileName;

    public CustomBotController(OpenAIService service, GmailApiClient gmailApiClient) {
        this.service = service;
        this.gmailApiClient = gmailApiClient;
    }


    @PostMapping("/upload")
    public String handleUpload(@RequestParam("pdfFile") MultipartFile pdfFile,
                               @RequestParam("OriginalFilename") String filename,
                               @RequestParam("prompt") String prompt,
                               @RequestParam(value = "password", required = false) String password,
                               Model model) {
        if (filename.equals("No file selected")) {
            try {
                if (pdfFile.isEmpty()) {
                    model.addAttribute("error", "pdf Should be there!!");
                    return "index";
                }
                ChatGptResponse response = service.callChatGptApi(pdfFile, prompt + " form given info", password);
                model.addAttribute("response", response.getChoices().get(0).getMessage().getContent());
                service.savePdf(pdfFile, prompt, response.getChoices().get(0).getMessage().getContent());
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        } else {
            try {
                String infoFormPdf = extractText;
                ChatGptResponse response = service.callChatGptApi(infoFormPdf, prompt + " form given info", password);
                model.addAttribute("response", response.getChoices().get(0).getMessage().getContent());
                service.savePdf(modifiedFileName, prompt, response.getChoices().get(0).getMessage().getContent());
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "index";
    }

    @GetMapping("/")
    public String home() throws Exception {
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
        List<Map<String, Object>> emails = EmailUtils.parseMessages(gmailApiClient.getGmail(), gmailApiClient.listEmails());
        model.addAttribute("emails", emails);
        return "index";
    }

    @GetMapping("/download/{messageId}/{filename}")
    public void downloadAttachment(
            @PathVariable String messageId,
            @PathVariable String filename,
            HttpServletResponse response
    ) throws IOException {
        modifiedFileName = generateNewFileName(filename);
        try {
            System.out.println(" gmailApiClient.getGmail() == " + gmailApiClient.getGmail());
            extractText = EmailUtils.downloadAttachment(gmailApiClient.getGmail(), messageId, filename, modifiedFileName, response);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Attachment not found.");
            response.getWriter().flush();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error downloading attachment.");
            response.getWriter().flush();
        }
    }


    public String generateNewFileName(String originalFileName) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return "file_" + timeStamp + "_" + originalFileName;
    }


}

