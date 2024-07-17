package com.broadcastMail.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadExcelDto {
    private String username;
    private String cc;
    private String password;
    private MultipartFile file;
    private MultipartFile zipFile;
    private String subject;
    private String body;
}

