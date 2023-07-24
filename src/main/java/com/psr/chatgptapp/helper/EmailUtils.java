package com.psr.chatgptapp.helper;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EmailUtils {

    public static List<Map<String, Object>> parseMessages(Gmail gmail, List<Message> messages) throws IOException {
        List<Map<String, Object>> emails = new ArrayList<>();
        for (Message message : messages) {
            Map<String, Object> email = new HashMap<>();
            Message fullMessage = gmail.users().messages().get("me", message.getId()).setFormat("full").execute();
//            System.out.println("Message Payload " + fullMessage.getPayload());
            if (fullMessage.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("from")) {
                        email.put("from", header.getValue());
                        email.put("messageId", message.getId()); // Adding the messageId to the email data
                    } else if (header.getName().equalsIgnoreCase("subject")) {
                        email.put("title", header.getValue()); // Adding the email title
                    }
                }
            }

            if (fullMessage.getSnippet() != null) {
                email.put("snippet", fullMessage.getSnippet());
            }

            // Process attachments (if any)
            List<Map<String, String>> attachments = processAttachments(fullMessage.getPayload(), gmail, message.getId());
            email.put("attachments", attachments);
            emails.add(email);
        }
        return emails;
    }

    private static List<Map<String, String>> processAttachments(MessagePart part, Gmail gmail, String messageId) throws IOException {
        List<Map<String, String>> attachments = new ArrayList<>();
        String mimeType = part.getMimeType();

        if (part.getFilename() != null && mimeType.startsWith("application/")) {
            // Attachment found
            String filename = part.getFilename();
            String attachmentId = getAttachmentId(gmail, messageId, part.getPartId()); // Get the attachmentId using a helper method
            Map<String, String> attachmentInfo = new HashMap<>();
            attachmentInfo.put("filename", filename);
            attachmentInfo.put("attachmentId", attachmentId);
            attachments.add(attachmentInfo);
        }

        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                attachments.addAll(processAttachments(childPart, gmail, messageId));
            }
        }

        return attachments;
    }

    private static String getAttachmentId(Gmail gmail, String messageId, String attachmentPartId) throws IOException {
        Message message = gmail.users().messages().get("me", messageId).execute();
        List<MessagePart> parts = message.getPayload().getParts();
        for (MessagePart part : parts) {
            if (attachmentPartId.equals(part.getPartId())) {
                return part.getBody().getAttachmentId();
            }
        }
        throw new IllegalArgumentException("Attachment with ID " + attachmentPartId + " not found in the message.");
    }



    public static void downloadAttachment(Gmail gmail, String messageId, String filename, HttpServletResponse response) throws IOException {
        Message message = gmail.users().messages().get("me", messageId).setFormat("full").execute();
        MessagePart attachmentPart = findPartByFilename(message.getPayload(), filename);
        if (attachmentPart == null) {
            throw new IllegalArgumentException("Attachment with filename " + filename + " not found in the message.");
        }

        String attachmentId = attachmentPart.getBody().getAttachmentId();
        System.out.println("attachmentId=========="+attachmentId);

        if (attachmentId != null) {
            // Attachment data is in a separate part, fetch it using the attachmentId
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MessagePartBody attachmentPartBody = gmail.users().messages().attachments().get("me", messageId, attachmentId).execute();
            byte[] fileByteArray = Base64.decodeBase64(attachmentPartBody.getData());

            // Set the response headers
            response.setContentType(attachmentPart.getMimeType());
            response.setContentLength(fileByteArray.length);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(fileByteArray);
            }
        } else {
            if (attachmentPart.getBody().getData() != null) {
                byte[] fileByteArray = Base64.decodeBase64(attachmentPart.getBody().getData());

                response.setContentType(attachmentPart.getMimeType());
                response.setContentLength(fileByteArray.length);
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

                try (OutputStream outputStream = response.getOutputStream()) {
                    outputStream.write(fileByteArray);
                }
            } else {
                throw new IllegalArgumentException("Attachment data not found.");
            }
        }
    }

    private static MessagePart findPartByFilename(MessagePart messagePart, String filename) {
        // This method searches for a specific part in the message payload by its filename
        if (filename.equals(messagePart.getFilename())) {
            return messagePart;
        } else if (messagePart.getParts() != null) {
            for (MessagePart part : messagePart.getParts()) {
                MessagePart foundPart = findPartByFilename(part, filename);
                if (foundPart != null) {
                    return foundPart;
                }
            }
        }
        return null;
    }

    private static MessagePart findPartById(MessagePart messagePart, String attachmentId) {
        // This method searches for a specific part in the message payload by its attachmentId
        if (attachmentId.equals(messagePart.getBody().getAttachmentId())) {
            System.out.println("messagePart=========="+messagePart);
            return messagePart;
        } else if (messagePart.getParts() != null) {
            for (MessagePart part : messagePart.getParts()) {
                MessagePart childPart = findPartById(part, attachmentId);
                if (childPart != null) {
                    System.out.println("childPart=========="+childPart);
                    return childPart;
                }
            }
        }
        return null;
    }


}