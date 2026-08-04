package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DlqMessageRepository extends JpaRepository<DlqMessage, Long> {

    Optional<DlqMessage> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
