package com.broadcastMail.repository;

import com.broadcastMail.entites.MailCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailCredentialsRepository extends JpaRepository<MailCredentials,Long> {
    MailCredentials findByMailId(String mailId);
}
