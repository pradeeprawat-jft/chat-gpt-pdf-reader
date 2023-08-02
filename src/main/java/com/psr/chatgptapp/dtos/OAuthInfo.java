package com.psr.chatgptapp.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OAuthInfo {
    private String oauthProvider;
    private String oauthId;
}