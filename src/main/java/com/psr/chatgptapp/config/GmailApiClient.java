package com.psr.chatgptapp.config;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class GmailApiClient {
    private static final String APPLICATION_NAME = "ChatGptAppApplication";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance(); // Initialize JSON_FACTORY
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/client_secret_64365572995-npss1r4sipkft8sr0oovvqcmcdmrkh01.apps.googleusercontent.com.json";
    private static final String REDIRECT_URI = "http://localhost:8080/gmail/oauth/callback"; // Redirect URI after authentication

    private Gmail gmail;

    public String getAuthorizationUrl() throws Exception {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream credentialsStream = GmailApiClient.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(credentialsStream));
        AuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URI);
        return authorizationUrl.build();
    }

    public void authorize(String code) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        InputStream credentialsStream = GmailApiClient.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(credentialsStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        TokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(REDIRECT_URI)
                .execute();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientSecrets)
                .build()
                .setFromTokenResponse(response);
        gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Message> listEmails() throws IOException {
        if (gmail == null) {
            throw new IllegalStateException("GmailApiClient is not authorized.");
        }
        String user = "me";
        return gmail.users().messages().list(user).execute().getMessages();
    }

    public String getUserEmail() throws IOException {
        if (gmail == null) {
            throw new IllegalStateException("GmailApiClient is not authorized.");
        }
        String user = "me";
        Profile profile = gmail.users().getProfile(user).execute();
        return profile.getEmailAddress();
    }

    public Gmail getGmail() {
        return gmail;
    }
}
