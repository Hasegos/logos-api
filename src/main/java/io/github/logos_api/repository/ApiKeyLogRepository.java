package io.github.logos_api.repository;

import io.github.logos_api.model.ApiKeyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ApiKeyLogRepository extends JpaRepository<ApiKeyLog, Long> {

    long countByIssuedIpAndCreatedAtAfter(String issuedIp, LocalDateTime startOfToday);

    @Modifying
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime timestamp);
}