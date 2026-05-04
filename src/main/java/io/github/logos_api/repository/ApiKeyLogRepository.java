package io.github.logos_api.repository;

import io.github.logos_api.model.ApiKeyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * API 키 로그 엔티티에 대한 데이터베이스 접근 인터페이스입니다.
 */
public interface ApiKeyLogRepository extends JpaRepository<ApiKeyLog, Long> {

    /**
     * 특정 IP 주소와 기준 시간 이후에 생성된 로그의 개수를 조회합니다.
     *
     * @param issuedIp 조회할 IP 주소
     * @param startOfToday 기준 시간 (오늘 자정 등)
     * @return 로그 개수
     */
    long countByIssuedIpAndCreatedAtAfter(String issuedIp, LocalDateTime startOfToday);

    /**
     * 지정된 시간 이전에 생성된 모든 API 키 로그를 삭제합니다.
     * @Modifying을 사용하여 데이터 변경 작업임을 명시하며, @Transactional을 통해 트랜잭션을 보장합니다.
     *
     * @param timestamp 삭제 기준이 되는 과거 시점의 시간
     */
    @Modifying
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime timestamp);
}