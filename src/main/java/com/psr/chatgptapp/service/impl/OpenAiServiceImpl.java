package com.psr.chatgptapp.service.impl;
import com.psr.chatgptapp.dtos.ChatGPTRequest;
import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.service.OpenAIService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Collections;

@Service
public class OpenAiServiceImpl implements OpenAIService {

    @Value(("${openai.api.url}"))
    private String apiURL;

    @Value(("${openai.model}"))
    private String myModel;

    @Value(("${openai.api.key}"))
    private String myKey;

    private final RestTemplate template;
    public OpenAiServiceImpl(RestTemplate template) {
        this.template = template;
    }

    @Override
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (
                PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        }
    }
    @Override
    public ChatGptResponse chat (String prompt, String bearerToken){
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", bearerToken);
        ChatGPTRequest request = new ChatGPTRequest(myModel, prompt);
        HttpEntity<ChatGPTRequest> httpRequest = new HttpEntity<>(request, headers);
        ResponseEntity<ChatGptResponse> response = template.postForEntity(apiURL, httpRequest, ChatGptResponse.class);
        return response.getBody();
    }
    @Override
    public ChatGptResponse callChatGptApi (MultipartFile file, String prompt) throws IOException {
        String extractedText = extractTextFromPdf(file);
        return chat(extractedText + " " + prompt, "Bearer " + myKey);
    }
}
