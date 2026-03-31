package io.github.logos_api.repository;

import io.github.logos_api.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    boolean existsByApiKey(String key);

    @Modifying
    @Transactional
    @Query("DELETE FROM ApiKey a where a.issuedIp = :issuedIp")
    void deleteByIssuedIp(String issuedIp);
}