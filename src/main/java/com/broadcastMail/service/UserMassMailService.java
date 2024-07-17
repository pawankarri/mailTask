package com.broadcastMail.service;

import com.broadcastMail.dto.PasswordDto;
import com.broadcastMail.dto.UploadExcelDto;
import com.broadcastMail.exception.FolderDoesNotExistsException;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.Map;

public interface UserMassMailService {
    Map<String, Object> massMails(UploadExcelDto excelDto) throws IOException;

    Map<String, Object> deleteFolder(String folderName) throws IOException, FolderDoesNotExistsException;

    Map<String, Object> getAllFolders();

    Map<String, Object> forgotPassword() throws MessagingException, IOException;

    Map<String, Object> resetPassword(PasswordDto passwordDto);


    Map<String, Object> getAllFiles(String folderName);

    Map<String, Object> deleteFile(String folderName, String fileName);

    Map<String, Object> getUnsentMailIds();
}
