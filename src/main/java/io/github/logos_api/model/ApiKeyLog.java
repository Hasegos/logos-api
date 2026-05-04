package io.github.logos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * API 키 발급 이력을 기록하는 엔티티 클래스입니다.
 * 일일 발급 횟수 제한 확인을 위해 사용됩니다.
 */
@Entity
@Table(name = "api_key_logs")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApiKeyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "issued_ip")
    private String issuedIp;

    public ApiKeyLog(String issuedIp){
        this.issuedIp = issuedIp;
    }
}
