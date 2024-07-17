package com.broadcastMail.serviceImpl;

import com.broadcastMail.config.EmailConfig;
import com.broadcastMail.dto.SigninDto;
import com.broadcastMail.entites.MailCredentials;
import com.broadcastMail.entites.User;
import com.broadcastMail.exception.UserAlreadyExistsException;
import com.broadcastMail.repository.MailCredentialsRepository;
import com.broadcastMail.repository.UserRepository;
import com.broadcastMail.service.UserMailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class UserMailServiceImpl implements UserMailService {
    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder encoder;


    @Autowired
    private MailCredentialsRepository mailCredentialsRepository;
    @Autowired
    private EmailConfig emailConfig;

    @Value("${files.storage}")
    public String folderLocation;


    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    Map<String, Object> map = new HashMap<>();


    @Override
    public Map<String, Object> createUser(SigninDto signinDto) {
        User user1 = this.repository.findByMailId(signinDto.getMailId());
        if (user1 != null) {
            throw new UserAlreadyExistsException("customer is alresady exists with this mail Id");
        }

        User user2 = new User();
        user2.setMailId(signinDto.getMailId());
        user2.setPassword(encoder.encode(signinDto.getPassword()));
        user2.setRole("user");
        this.repository.save(user2);
        map.put("status", HttpStatus.CREATED.value());
        map.put("mesage", "created successfully");
        map.put("result", user2);
        return map;
    }

    @Override
    public Map<String, Object> saveMailCredentials(MailCredentials credentials) {
        MailCredentials mailCredentials = this.mailCredentialsRepository.findByMailId(credentials.getMailId());
        if (mailCredentials != null) {
            throw new UserAlreadyExistsException("this mail credentials already exists");
        }
        MailCredentials mailCredentials1 = new MailCredentials();
        mailCredentials1.setMailId(credentials.getMailId());
        mailCredentials1.setPassword(credentials.getPassword());
        this.mailCredentialsRepository.save(mailCredentials1);
        map.put("status", HttpStatus.CREATED.value());
        map.put("message", "created successfully");
        map.put("result", mailCredentials1);
        return map;
    }

    @Override
    public Map<String, Object> getAllMailCredentials() {
        List<MailCredentials> list = this.mailCredentialsRepository.findAll();
        map.put("status", HttpStatus.OK.value());
        map.put("message", "fetched successfully");
        map.put("result", list);
        return map;
    }


    @Async
    private void sendingMailsOneByOne(String username, String password, String name, String emails, String appraisalType, String appraisalScore, String dueDate, String subject, List<MultipartFile> attachments) throws Exception {
        JavaMailSender javaMailSender = emailConfig.getJavaMailSender(username, password);
        MimeMessage mail = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mail, true);

        mimeMessageHelper.addTo(emails);
        String template = readingMailTemplateFromText("appraisal-mail.txt");
        String processedTemplate = template.replace("@empName", name).replace("@typeOfAppraisal", appraisalType).replace("@appraisalScore", appraisalScore).replace("@dueDate", dueDate.toString());
        mimeMessageHelper.setFrom(username);
        mimeMessageHelper.setSubject("[Broadcast] " + subject);

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile attachment : attachments) {
                if (attachment != null && !attachment.isEmpty()) {
                    mimeMessageHelper.addAttachment(Objects.requireNonNull(attachment.getOriginalFilename()), new ByteArrayResource(attachment.getBytes()));
                }
            }
        }

        mimeMessageHelper.setText(processedTemplate, true);
        javaMailSender.send(mail);
    }

    private void sendingMailsOneByOneWithoutAttachments(String username, String password, String name, String emails, String appraisalType, String appraisalScore, String dueDate, String subject) throws Exception {
        JavaMailSender javaMailSender = emailConfig.getJavaMailSender(username, password);
        MimeMessage mail = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mail, true);

        mimeMessageHelper.addTo(emails);
        String template = readingMailTemplateFromText("appraisal-mail.txt");
        String processedTemplate = template.replace("@empName", name).replace("@typeOfAppraisal", appraisalType).replace("@appraisalScore", appraisalScore).replace("@dueDate", dueDate.toString());
        mimeMessageHelper.setFrom(username);
        mimeMessageHelper.setSubject("[Broadcast] " + subject);
        mimeMessageHelper.setText(processedTemplate, true);

        javaMailSender.send(mail);
    }

    private String readingMailTemplateFromText(String fileName) throws Exception {
        try {
            String fullFileName = folderLocation + "/" + fileName;
            File file = new File(fullFileName);
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new Exception("Mail not sent");
        }
    }



}
