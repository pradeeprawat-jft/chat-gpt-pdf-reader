package com.psr.chatgptapp.service.impl;
import com.psr.chatgptapp.dtos.ChatGPTRequest;
import com.psr.chatgptapp.dtos.ChatGptResponse;
import com.psr.chatgptapp.entity.Query;
import com.psr.chatgptapp.helper.SavePDF;
import com.psr.chatgptapp.repo.QueryRepo;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;


@Service
public class OpenAiServiceImpl implements OpenAIService {

    @Value(("${openai.api.url}"))
    private String apiURL;

    @Value(("${openai.model}"))
    private String myModel;

    @Value(("${openai.api.key}"))
    private String myKey;

    private final SavePDF fileUpload;
    private final QueryRepo repo;

    private final RestTemplate template;
    public OpenAiServiceImpl(SavePDF fileUpload, QueryRepo repo, RestTemplate template) {
        this.fileUpload = fileUpload;
        this.repo = repo;
        this.template = template;
    }

    @Override
    public String extractTextFromPdf(MultipartFile file, String password) throws IOException {
        try {
            InputStream inputStream = file.getInputStream();
            PDDocument document;
            if (password != null && !password.isEmpty()) {
                document = PDDocument.load(inputStream, password);
            } else {
                document = PDDocument.load(inputStream);
            }
            PDFTextStripper textStripper = new PDFTextStripper();
            String extractedText = textStripper.getText(document);
            document.close();
            return extractedText;
        } catch (IOException e) {
            e.printStackTrace();
            throw  e;
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
    public ChatGptResponse callChatGptApi (MultipartFile file, String prompt,String password) throws IOException {
        try {
            String extractedText = extractTextFromPdf(file,password);
            return chat(extractedText + " " + prompt, "Bearer " + myKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean savePdf(MultipartFile pdfFile) {
        Query info = new Query();
        String newFileName = null;
        boolean status;
        try {
            newFileName = generateNewFileName(pdfFile.getOriginalFilename());
            status = fileUpload.fileUpload(pdfFile,newFileName);
            info.setImage(newFileName);
            repo.save(info);
        } catch (Exception e) {
            return false;
        }
        return status;
    }

    @Override
    public String generateNewFileName(String originalFileName) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return "file_" + timeStamp + "_" + originalFileName;
    }

}
