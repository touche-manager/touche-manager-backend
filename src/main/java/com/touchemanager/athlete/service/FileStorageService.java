package com.touchemanager.athlete.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String directory);
    InputStream downloadFile(String key);
    void deleteFile(String key);
}
