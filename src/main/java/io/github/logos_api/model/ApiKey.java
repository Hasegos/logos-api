package io.github.logos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 발급된 API 키 정보를 담는 엔티티 클래스입니다.
 * 키 원문은 발급할 때 한 번만 사용자에게 보여 주고, 여기에는 해시만 남깁니다.
 * JPA Auditing을 사용하여 생성 시간을 자동으로 기록합니다.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 발급한 키의 SHA-256 해시입니다. 원문은 저장하지 않습니다.
     */
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "issued_ip")
    private String issuedIp;
}