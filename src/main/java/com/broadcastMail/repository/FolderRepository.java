package com.broadcastMail.repository;

import com.broadcastMail.entites.FolderEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.File;
import java.util.List;

@Transactional
public interface FolderRepository extends JpaRepository<FolderEntity,Long> {
    void deleteByFolderName(String folder);


    FolderEntity findByFolderName(String extractedFileName);
}
