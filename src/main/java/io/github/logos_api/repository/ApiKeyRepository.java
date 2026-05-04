package io.github.logos_api.repository;

import io.github.logos_api.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 키 엔티티에 대한 데이터베이스 접근 및 관리를 담당하는 리포지토리 인터페이스입니다.
 * API 키의 유효성 검증과 특정 IP에 할당된 키의 삭제 기능을 제공합니다.
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * 데이터베이스에 해당 API 키가 존재하는지 여부를 확인합니다.
     *
     * @param key 검증할 API 키 문자열
     * @return 키가 존재하면 true, 존재하지 않으면 false 반환
     */
    boolean existsByApiKey(String key);

    /**
     * 특정 IP 주소로 발급된 기존 API 키를 삭제합니다.
     * JPQL을 사용하여 직접 삭제 쿼리를 수행하며, 트랜잭션 내에서 실행됩니다.
     *
     * @param issuedIp 삭제 대상이 되는 키를 발급받은 IP 주소
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ApiKey a where a.issuedIp = :issuedIp")
    void deleteByIssuedIp(String issuedIp);
}