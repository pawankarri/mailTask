package com.broadcastMail.controller;

import com.broadcastMail.dto.SigninDto;
import com.broadcastMail.dto.UploadExcelDto;
import com.broadcastMail.entites.MailCredentials;
import com.broadcastMail.service.UserMailService;
import com.broadcastMail.service.UserMassMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("api/v1/mail")
public class UserController {
    @Autowired
    private UserMailService mailService;
    @Autowired
    private UserMassMailService userMassMailService;

    @PostMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody SigninDto signinDto)
    {
        Map<String, Object> map=this.mailService.createUser(signinDto);
        return ResponseEntity.ok().body(map);
    }

    @PostMapping("/save-mail-Credentials")
    public ResponseEntity<Map<String, Object>> saveMailCredentials(@RequestBody MailCredentials credentials)
    {
        Map<String, Object> map=this.mailService.saveMailCredentials(credentials);
        return ResponseEntity.ok().body(map);
    }


    @GetMapping("/get-mail-credentials")
    public ResponseEntity<Map<String,Object>> getAllMailCredentials()
    {
        Map<String, Object> map=this.mailService.getAllMailCredentials();
        return ResponseEntity.ok().body(map);
    }


    @PostMapping("/upload-excel")
    public ResponseEntity<Map<String,Object>> sendMails(@ModelAttribute UploadExcelDto excelDto) throws IOException {
       Map<String,Object> map= this.userMassMailService.massMails(excelDto);
        return ResponseEntity.ok().body(map);
    }


    @DeleteMapping("/delete-folder/{folderName}")
    public ResponseEntity<Map<String,Object>> deleteFolder(@PathVariable String folderName) throws IOException {
        Map<String,Object> map=  this.userMassMailService.deleteFolder(folderName);
        return ResponseEntity.ok().body(map);
    }


    @GetMapping("/get-all-folders")
    public ResponseEntity<Map<String,Object>> getAllFolders()
    {
        Map<String,Object> map=  this.userMassMailService.getAllFolders();
        return ResponseEntity.ok().body(map);
    }

      @GetMapping("/get-all-files/{folderName}")
    public ResponseEntity<Map<String,Object>> getAllFiles(@PathVariable String folderName)
      {
          Map<String,Object> map=this.userMassMailService.getAllFiles(folderName);
          return ResponseEntity.ok().body(map);
      }


      @DeleteMapping("/delete-file/{folderName}/{fileName}")
          public ResponseEntity<Map<String,Object>> deleteFile(@PathVariable String folderName,@PathVariable String fileName)
      {
          Map<String,Object> map=this.userMassMailService.deleteFile(folderName,fileName);
          return ResponseEntity.ok().body(map);
      }



      @GetMapping("/get-all-unsentMailIds-fromLastMail")
          public ResponseEntity<Map<String,Object>> getUnsentMailIds()
          {
              Map<String,Object> map=  this.userMassMailService.getUnsentMailIds();
              return ResponseEntity.ok().body(map);
          }


}
