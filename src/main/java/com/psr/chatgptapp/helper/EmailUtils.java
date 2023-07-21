package com.psr.chatgptapp.helper;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailUtils {

    public static List<Map<String, Object>> parseMessages(Gmail gmail, List<Message> messages) throws IOException {
        List<Map<String, Object>> emails = new ArrayList<>();
        for (Message message : messages) {
            Map<String, Object> email = new HashMap<>();
            Message fullMessage = gmail.users().messages().get("me", message.getId()).setFormat("full").execute();
            if (fullMessage.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("from")) {
                        email.put("from", header.getValue());
                    } else if (header.getName().equalsIgnoreCase("subject")) {
                        email.put("title", header.getValue()); // Adding the email title
                    }
                }
            }

            if (fullMessage.getSnippet() != null) {
                email.put("snippet", fullMessage.getSnippet());
            }

            // Process attachments (if any)
            List<Map<String, String>> attachments = processAttachments(fullMessage.getPayload());
            email.put("attachments", attachments);

            emails.add(email);
        }
        return emails;
    }
    private static List<Map<String, String>> processAttachments(MessagePart part) {
        List<Map<String, String>> attachments = new ArrayList<>();
        String mimeType = part.getMimeType();
        if (part.getFilename() != null && mimeType.startsWith("application/")) {
            // Attachment found
            String filename = part.getFilename();
            String attachmentId = part.getBody().getAttachmentId(); // Get the attachmentId
            Map<String, String> attachmentInfo = new HashMap<>();
            attachmentInfo.put("filename", filename);
            attachmentInfo.put("attachmentId", attachmentId);
            attachments.add(attachmentInfo);
        }

        if (part.getParts() != null) {
            // Recursively process all nested parts (if any)
            for (MessagePart childPart : part.getParts()) {
                attachments.addAll(processAttachments(childPart));
            }
        }

        return attachments;
    }

    public static void downloadAttachment(Gmail gmail, String messageId, String attachmentId, HttpServletResponse response) throws IOException {
        Message message = gmail.users().messages().get("me", messageId).execute();
        MessagePart attachmentPart = null;
        for (MessagePart part : message.getPayload().getParts()) {
            if (part.getPartId().equals(attachmentId)) {
                attachmentPart = part;
                break;
            }
        }

        if (attachmentPart == null) {
            throw new IllegalArgumentException("Attachment with ID " + attachmentId + " not found in the message.");
        }

        MessagePartBody attachmentPartBody = attachmentPart.getBody();
        byte[] fileByteArray = Base64.decodeBase64(attachmentPartBody.getData());
        InputStream inputStream = new ByteArrayInputStream(fileByteArray);

        response.setContentType(attachmentPart.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=" + attachmentPart.getFilename());

        try (OutputStream outputStream = response.getOutputStream()) {
            int readBytes;
            byte[] buffer = new byte[Math.min(fileByteArray.length, 8192)]; // Use a dynamic buffer size, here set to 8192 bytes (8KB)
            while ((readBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }
        }

        inputStream.close();
    }

}
