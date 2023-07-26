package com.psr.chatgptapp.helper;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class SavePDF {
    public final String UPLOAD_DIR;

    public SavePDF() {
        UPLOAD_DIR = "/home/jellyfish/Desktop/chat-gpt-app/src/main/resources/static/pdf";
    }
    public boolean fileUpload(MultipartFile file,String newFileName) {
        boolean status = false;
        try {
            Files.copy(file.getInputStream(),
                    Paths.get(UPLOAD_DIR + File.separator + newFileName),
                    StandardCopyOption.REPLACE_EXISTING);
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }


}
