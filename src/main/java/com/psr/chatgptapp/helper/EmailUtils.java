package com.psr.chatgptapp.helper;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EmailUtils {

    public static final String SPECIFIC_GMAIL_ADDRESS = "pradeep2000rawat@gmail.com";

    public static List<Map<String, Object>> parseMessages(Gmail gmail, List<Message> messages) throws IOException {
        List<Map<String, Object>> emails = new ArrayList<>();
        for (Message message : messages) {
            Map<String, Object> email = new HashMap<>();
            Message fullMessage = gmail.users().messages().get("me", message.getId()).setFormat("full").execute();

            if (fullMessage.getPayload().getHeaders() != null) {
                String fromAddress = null;
                String subject = null;

                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("from")) {
                        fromAddress = extractEmailAddress(header.getValue());
                        // Check if the from address matches the specific Gmail address
                        if (fromAddress != null && fromAddress.equals(SPECIFIC_GMAIL_ADDRESS)) {
                            email.put("from", fromAddress);
                            email.put("messageId", message.getId());
                        }
                    } else if (header.getName().equalsIgnoreCase("subject")) {
                        subject = header.getValue(); // Retrieve the subject here
                    }
                }

                // Check if the email belongs to the specific Gmail address before adding it to the list
                if (fromAddress != null && fromAddress.equals(SPECIFIC_GMAIL_ADDRESS)) {
                    // Add the title, snippet, and attachments
                    email.put("title", subject);
                    if (fullMessage.getSnippet() != null) {
                        email.put("snippet", fullMessage.getSnippet());
                    }
                    List<Map<String, String>> attachments = processAttachments(fullMessage.getPayload(), gmail, message.getId());
                    email.put("attachments", attachments);

                    // Add the email to the list
                    emails.add(email);
                }
            }
        }
        return emails;
    }

    // Helper method to extract the email address from the "From" header
    private static String extractEmailAddress(String fromHeader) {
        int startIndex = fromHeader.lastIndexOf('<');
        int endIndex = fromHeader.lastIndexOf('>');

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return fromHeader.substring(startIndex + 1, endIndex).trim();
        } else {
            return null;
        }
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
        throw new IllegalArgumentException("Attachment with ID " + attachmentPartId + "not found in the message.");
    }

    public static String downloadAttachment(Gmail gmail, String messageId, String filename, String renamedFile, HttpServletResponse response) throws IOException {
        final String saveFolderPath = "/home/jellyfish/Desktop/chat-gpt-app/src/main/resources/static/pdf";
        Message message = gmail.users().messages().get("me", messageId).setFormat("full").execute();
        MessagePart attachmentPart = findPartByFilename(message.getPayload(), filename);
        if (attachmentPart == null) {
            throw new IllegalArgumentException("Attachment with filename " + filename + " not found in the message.");
        }
        String attachmentId = attachmentPart.getBody().getAttachmentId();
        System.out.println("attachmentId==========" + attachmentId);
        if (attachmentId != null) {
            MessagePartBody attachmentPartBody = gmail.users().messages().attachments().get("me", messageId, attachmentId).execute();
            byte[] fileByteArray = Base64.decodeBase64(attachmentPartBody.getData());

            String saveFilePath = saveFolderPath + File.separator + renamedFile;
            try (OutputStream outputStream = new FileOutputStream(saveFilePath)) {
                outputStream.write(fileByteArray);
            }
            // Set the response headers
            response.setContentType(attachmentPart.getMimeType());
            response.setContentLength(fileByteArray.length);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(fileByteArray);

            }
        }
//        else {
//            if (attachmentPart.getBody().getData() != null) {
//                byte[] fileByteArray = Base64.decodeBase64(attachmentPart.getBody().getData());
//
//                // Save the attachment to the specified folder
//                String saveFilePath = saveFolderPath + File.separator + generateNewFileName(filename);
//                try (OutputStream outputStream = new FileOutputStream(saveFilePath)) {
//                    outputStream.write(fileByteArray);
//                }
//
//                response.setContentType(attachmentPart.getMimeType());
//                response.setContentLength(fileByteArray.length);
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
//
//                try (OutputStream outputStream = response.getOutputStream()) {
//                    outputStream.write(fileByteArray);
//                }
//            } else {
//                throw new IllegalArgumentException("Attachment data not found.");
//            }
//        }
        return extractText(renamedFile);
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


    public static String extractText(String filename) {
        String text = "";
        try {
            File file = new File("/home/jellyfish/Desktop/chat-gpt-app/src/main/resources/static/pdf/" + filename);
            PDDocument document = PDDocument.load(file);
            // Create PDFTextStripper instance to extract text
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            text = pdfTextStripper.getText(document);
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }
}