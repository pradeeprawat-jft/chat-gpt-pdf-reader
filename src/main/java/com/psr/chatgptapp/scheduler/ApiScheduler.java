package com.psr.chatgptapp.scheduler;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@EnableScheduling
public class ApiScheduler {

    private final RestTemplate restTemplate;


    public ApiScheduler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedDelay =  (1000 * 60 * 3), initialDelay = (1000 * 60 * 2))
    public void callApiWithParameters() {
        String apiUrl = "http://localhost:8080/gmail/emails";
        ResponseEntity<Void> response = restTemplate.exchange(apiUrl, HttpMethod.GET, null, Void.class);
        System.out.println(response);
        System.out.println("======================================scheduler starter==========================================");
    }

}