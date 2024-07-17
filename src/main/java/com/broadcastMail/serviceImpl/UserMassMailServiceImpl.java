package com.broadcastMail.serviceImpl;

import com.broadcastMail.config.EmailConfig;
import com.broadcastMail.dto.PasswordDto;
import com.broadcastMail.dto.UploadExcelDto;
import com.broadcastMail.entites.FolderEntity;
import com.broadcastMail.entites.UnSentMailIds;
import com.broadcastMail.entites.User;
import com.broadcastMail.exception.FolderDoesNotExistsException;
import com.broadcastMail.exception.InvalidFileExtensionException;
import com.broadcastMail.repository.FolderRepository;
import com.broadcastMail.repository.UnSentMailIdsRepository;
import com.broadcastMail.repository.UserRepository;
import com.broadcastMail.service.UserMassMailService;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class UserMassMailServiceImpl implements UserMassMailService {

    @Autowired
    private EmailConfig emailConfig;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnSentMailIdsRepository unSentMailIdsRepository;

    @Value("${files.storage}")
    public String folderLocation;

    @Value("${spring.mail.username}")
    public String mail;
    @Value("${spring.mail.password}")
    public String mailPassword;

    public static final String forgot_password_sent_mail="karriramakrishna.pavankumar@eidiko-india.com";

    public static final String Email_Id="emailId";

    public static final String File_Name="filename";

    public static final String Resource_Path="classpath:/templates/forgotPassword.html";

    public static final String Subject="Forgot-password";

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    Map<String, Object> resultMap = new HashMap<>();
    List<String> notSentEmails = new CopyOnWriteArrayList<>();
    List<String> notFoundAddresses = new CopyOnWriteArrayList<>();

    List<String> allMailIds=new CopyOnWriteArrayList<>();
   List<String>  sentMailIds=new CopyOnWriteArrayList<>();


   public int count=0;


    @Override
    public Map<String, Object> massMails(UploadExcelDto excelDto) throws IOException {
        String extractedFileName = unZipFile2(excelDto.getZipFile());
        log.info("The Following Folder was {} unZipped", extractedFileName);

        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderName(extractedFileName);
        FolderEntity folder = this.folderRepository.findByFolderName(extractedFileName);

        if (folder == null) {
            this.folderRepository.save(folderEntity);
        }

        String fileName = excelDto.getFile().getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(fileName);
        List<String> allowedExtensions = Arrays.asList("xls", "xlsx", "csv");

        if (!allowedExtensions.contains(fileExtension)) {
            throw new InvalidFileExtensionException("Please provide an Excel file.");
        }

        XSSFWorkbook workbook = new XSSFWorkbook(excelDto.getFile().getInputStream());
        XSSFSheet sheet = workbook.getSheetAt(0);
        List<Callable<Void>> emailTasks = new ArrayList<>();

        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                List<String> entireRowCells = new ArrayList<>();
                List<String> filenameAndEmpidCells = new ArrayList<>();

                for (int j = 0; j < row.getLastCellNum(); j++) {
                    XSSFCell cell = row.getCell(j);
                    if (cell != null) {
                        String cellValue;
                        switch (cell.getCellType()) {
                            case STRING:
                                cellValue = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                double numericValue = cell.getNumericCellValue();
                                if (numericValue == (int) numericValue) {
                                    cellValue = Integer.toString((int) numericValue);
                                } else {
                                    cellValue = Double.toString(numericValue);
                                }
                                break;
                            case BOOLEAN:
                                cellValue = Boolean.toString(cell.getBooleanCellValue());
                                break;
                            case FORMULA:
                                cellValue = cell.getCellFormula();
                                break;
                            case BLANK:
                                cellValue = "";
                                break;
                            default:
                                cellValue = cell.toString();
                                break;
                        }
                        String columnName = getColumnName(sheet.getRow(0).getCell(j));

                        if (columnName.equalsIgnoreCase(Email_Id) || columnName.equalsIgnoreCase(File_Name)) {
                            filenameAndEmpidCells.add(cellValue);
                        }
                        entireRowCells.add(cellValue);
                    }
                }

                allMailIds.add(filenameAndEmpidCells.get(0));
                log.info("All MailIds from Excel sheet {}", allMailIds);

                emailTasks.add(() -> {
                    int retryCount = 0;
                    int maxRetries = 5;
                    long retryDelay = 1000;
                    while (retryCount < maxRetries) {
                        try {
                            Thread.sleep(retryDelay);
                            sendEmail(excelDto, filenameAndEmpidCells, entireRowCells, extractedFileName);
                            sentMailIds.add(filenameAndEmpidCells.get(0));
                            return null;
                        } catch (MessagingException e) {
                            if (e.getMessage().contains("Invalid Addresses")) {
                                notFoundAddresses.add(filenameAndEmpidCells.get(0));
                                return null;
                            } else if (e.getMessage().contains("451 4.3.0")) {
                                retryCount++;
                                retryDelay *= 2;
                                log.warn("Temporary failure, retrying... Attempt: {}", retryCount);
                            } else {
                                notSentEmails.add(filenameAndEmpidCells.get(0));
                                return null;
                            }
                        } catch (IOException e) {
                            notSentEmails.add(filenameAndEmpidCells.get(0));
                            return null;
                        } catch (Exception e) {
                            notSentEmails.add(filenameAndEmpidCells.get(0));
                            return null;
                        }
                    }
                    notSentEmails.add(filenameAndEmpidCells.get(0));
                    return null;
                });
            }
        }

        try {
            executorService.invokeAll(emailTasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending was interrupted", e);
        }

        allMailIds.removeAll(sentMailIds);
        List<String> remainingMailIds = new ArrayList<>(allMailIds);

        unSentMailIdsRepository.deleteAll();
        List<UnSentMailIds> unSentMailIdsList = new ArrayList<>();
        if (!remainingMailIds.isEmpty()) {

            for (String mailId : remainingMailIds) {
                UnSentMailIds unSentMailIds = new UnSentMailIds();
                unSentMailIds.setMailId(mailId);
                unSentMailIdsList.add(unSentMailIds);
            }
            unSentMailIdsRepository.saveAll(unSentMailIdsList);

        }
        log.info("All unSentMailIds are saved: {}", unSentMailIdsList);
        resultMap.put("status", HttpStatus.CREATED.value());
        resultMap.put("message", "Mail sent successfully");
        return resultMap;
    }

    private String getColumnName(XSSFCell cell) {
        if (cell != null) {
            return cell.getStringCellValue().trim();
        }
        return "";
    }

    private void sendEmail(UploadExcelDto excelDto, List<String> emailAndFilenameList, List<String> cellValueList, String extractedFileName) throws IOException, MessagingException {
        if (emailAndFilenameList != null && !emailAndFilenameList.isEmpty()) {
            JavaMailSender javaMailSender = emailConfig.getJavaMailSender(excelDto.getUsername(), excelDto.getPassword());
            MimeMessage mail = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mail, true);
            String recipientEmail = emailAndFilenameList.get(0);
            String body = excelDto.getBody();

            String[] lines = body.split("\\r?\\n");
            StringBuilder formattedBody = new StringBuilder();

            for (String line : lines) {
                String formattedLine = line;
                for (int i = 0; i < cellValueList.size(); i++) {
                    String placeholder = "{" + i + "}";
                    formattedLine = formattedLine.replace(placeholder, cellValueList.get(i));
                }
                formattedLine = formattedLine.replaceAll("@(.*?)@", "<b>$1</b>");
                formattedLine = formattedLine.replaceAll("#(.*?)#", "<span style=\"background-color: yellow;\">$1</span>");
                formattedBody.append(formattedLine).append("<br/>");
            }

            try {
                mimeMessageHelper.setTo(recipientEmail);
            } catch (MessagingException e) {
                notFoundAddresses.add(recipientEmail);
                throw e;
            }

            if (excelDto.getCc() != null && !excelDto.getCc().trim().isEmpty()) {
                mimeMessageHelper.setCc(parseAddresses(excelDto.getCc()));
            }
            mimeMessageHelper.setText(formattedBody.toString(), true);
            mimeMessageHelper.setFrom(excelDto.getUsername());
            mimeMessageHelper.setSubject(excelDto.getSubject());

            if (emailAndFilenameList.size() > 1) {
                String filenames = emailAndFilenameList.get(1);
                String[] filenameArray = filenames.split(",");
                for (String fileName : filenameArray) {
                    fileName = fileName.trim();
                    File file = searchFileInLocalDirectory(extractedFileName, fileName);
                    if (file != null && file.exists()) {
                        FileSystemResource fileSystemResource = new FileSystemResource(file);
                        mimeMessageHelper.addAttachment(file.getName(), fileSystemResource);
                    }
                }
            }

            try {
                Thread.sleep(1000);
                javaMailSender.send(mail);
                log.info("Sent mail for this mail ID: {}", recipientEmail);
                sentMailIds.add(recipientEmail);
            } catch (MailSendException me) {
                me.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private InternetAddress[] parseAddresses(String cc) throws AddressException {
        return InternetAddress.parse(cc);
    }

    private File searchFileInLocalDirectory(String folderName, String fileName) {
        String directoryPath = folderLocation + "/" + folderName;
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.equals(fileName));
            if (files != null && files.length > 0) {
                return files[0];
            }
        }
        return null;
    }

    private String unzipFile(MultipartFile zipFile) throws IOException {
        File folder = new File(folderLocation);
        if (!folder.exists()) {
            folder.mkdir();
        }
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(zipFile.getInputStream());
        ZipEntry zipEntry = zis.getNextEntry();
        String folderName = null;
        while (zipEntry != null) {
            File newFile = new File(folder, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                if (!newFile.exists()) {
                    newFile.mkdirs();
                }
                if (folderName == null) {
                    folderName = newFile.getName();
                }
            } else {
                File parentDir = newFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return folderName;
    }

    private String unZipFile2(MultipartFile file) throws IOException {
        Path destination = Paths.get(folderLocation).normalize();
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(file.getInputStream());
        ZipEntry zipEntry = zis.getNextEntry();
        String folderName = null;
        while (zipEntry != null) {
            Path path = destination.resolve(zipEntry.getName()).normalize();
            if (!path.startsWith(destination)) {
                zipEntry = zis.getNextEntry();
                continue;
            }
            File newFile = path.toFile();
            if (zipEntry.isDirectory()) {
                if (!newFile.exists()) {
                    newFile.mkdirs();
                }
                if (folderName == null) {
                    folderName = newFile.getName();
                }
            } else {
                File parentDir = newFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(newFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                }
            }
            zis.closeEntry();
            zipEntry = zis.getNextEntry();
        }
        zis.close();
        return folderName;
    }



    public static String generateRandomPassword(int len, int randNumOrigin, int randNumBound) {
        SecureRandom random = new SecureRandom();
        return random.ints(randNumOrigin, randNumBound + 1)
                .filter(i -> Character.isAlphabetic(i) || Character.isDigit(i)).limit(len)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }



    @Override
    public Map<String, Object> forgotPassword() throws MessagingException, IOException {
        User signUp = this.userRepository.findByMailId(mail);
        if (signUp == null) {
            throw new UsernameNotFoundException("User with the provided email does not exist");
        }
        String password = generateRandomPassword(6, 48, 122);
        signUp.setPassword(encoder.encode(password));
        this.userRepository.save(signUp);
        JavaMailSender javaMailSender = emailConfig.getJavaMailSender(mail, mailPassword);
        MimeMessage mail = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mail, true);
        mimeMessageHelper.setTo(forgot_password_sent_mail);
        Resource resource = resourceLoader.getResource(Resource_Path);
        String templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String body = templateContent.replace("@[Password]", password);
        mimeMessageHelper.setText(body, true);
        mimeMessageHelper.setFrom(String.valueOf(mail));
        mimeMessageHelper.setSubject(Subject);
        javaMailSender.send(mail);
        resultMap.put("status", HttpStatus.CREATED.value());
        resultMap.put("message", "New password sent to email");

        return resultMap;
    }

    @Override
    public Map<String, Object> resetPassword(PasswordDto passwordDto) {

        User user = this.userRepository.findByMailId(passwordDto.getUsername());
        if (user == null) {
            throw new UsernameNotFoundException("Email Id doesn't Exist");
        } else {
            if (encoder.matches(passwordDto.getOldPassword(), user.getPassword())) {
                user.setPassword(encoder.encode(passwordDto.getNewPassword()));
                this.userRepository.save(user);
                resultMap.put("status", HttpStatus.CREATED.value());
                resultMap.put("message", "Password Reset Successfully");
            } else {
                throw new RuntimeException("Please provide the correct password");
            }
        }

        return resultMap;
    }

    @Override
    public Map<String, Object> getAllFiles(String folderName) {
        List<String> fileList = new ArrayList<>();
        String folderPath = folderLocation+"/"+folderName;
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file.getName());
            }
        }
        log.info("files in folder name:"+folderName+ fileList);
        resultMap.put("status",HttpStatus.OK.value());
        resultMap.put("message","fetched successfully");
        resultMap.put("result",fileList);
        return resultMap;
    }

    @Override
    public Map<String, Object> deleteFile(String folderName, String fileName) {
        String filePath=folderLocation+"/"+folderName+"/"+fileName;
        Path path = Paths.get(filePath);
        try {
            Files.delete(path);
            log.info("File Deleted : "+filePath);
            resultMap.put("status",HttpStatus.OK.value());
            resultMap.put("message","deleted successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> getUnsentMailIds() {
        List<UnSentMailIds> all = unSentMailIdsRepository.findAll();
        log.info("All Unsent Mails from last Mail : {}",all);
        resultMap.put("status",HttpStatus.OK.value());
        resultMap.put("message","Fetched Successfully");
        resultMap.put("result",all);
        return resultMap;
    }


    @Override
    public Map<String, Object> deleteFolder(String folderName) throws IOException {
        String fName=folderLocation+"/"+folderName;
        File folder = new File(fName);
        if (!folder.exists()) {
            throw new FolderDoesNotExistsException("File doesn't exists in your machine");
        }
        FileUtils.deleteDirectory(folder);
        log.info("Folder Deleted : "+folder);
        resultMap.put("status", HttpStatus.OK.value());
        resultMap.put("message", "Folder Deleted successfully");
        this.folderRepository.deleteByFolderName(folderName);
        return resultMap;
    }

    @Override
    public Map<String, Object> getAllFolders() {
        List<FolderEntity>  list= this.folderRepository.findAll();
        log.info("All Folders are Fetched successfully :"+list);
        resultMap.put("status",HttpStatus.OK.value());
        resultMap.put("message","fetched successfully");
        resultMap.put("result",list);
        return resultMap;
    }


}
