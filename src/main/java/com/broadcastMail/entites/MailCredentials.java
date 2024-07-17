package com.broadcastMail.entites;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long mailCredentialsId;
    private String mailId;
    private String password;
}
