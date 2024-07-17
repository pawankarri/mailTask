package com.broadcastMail.repository;

import com.broadcastMail.entites.UnSentMailIds;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnSentMailIdsRepository extends JpaRepository<UnSentMailIds,Long> {
}
