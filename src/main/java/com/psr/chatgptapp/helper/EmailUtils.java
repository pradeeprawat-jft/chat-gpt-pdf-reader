package com.psr.chatgptapp.helper;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailUtils {

    public static List<Map<String, String>> parseMessages(Gmail gmail, List<Message> messages) throws IOException {
        List<Map<String, String>> emails = new ArrayList<>();
        for (Message message : messages) {
            Map<String, String> email = new HashMap<>();
            Message fullMessage = gmail.users().messages().get("me", message.getId()).setFormat("full").execute();
            if (fullMessage.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("from")) {
                        email.put("from", header.getValue());
                        break;
                    }
                }
            }

            if (fullMessage.getSnippet() != null) {
                email.put("snippet", fullMessage.getSnippet());
            }

            // Process attachments (if any)
            List<String> attachments = processAttachments(fullMessage.getPayload());
            email.put("attachments", attachments.toString());

            emails.add(email);
        }
        return emails;
    }

    private static List<String> processAttachments(MessagePart part) {
        List<String> attachments = new ArrayList<>();
        String mimeType = part.getMimeType();
        if (part.getFilename() != null && mimeType.startsWith("application/")) {
            // Attachment found
            String filename = part.getFilename();
            attachments.add(filename);
        }

        if (part.getParts() != null) {
            // Recursively process all nested parts (if any)
            for (MessagePart childPart : part.getParts()) {
                attachments.addAll(processAttachments(childPart));
            }
        }

        return attachments;
    }
}
