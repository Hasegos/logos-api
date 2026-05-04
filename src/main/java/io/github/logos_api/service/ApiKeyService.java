package io.github.logos_api.service;

import io.github.logos_api.model.ApiKey;
import io.github.logos_api.model.ApiKeyLog;
import io.github.logos_api.repository.ApiKeyLogRepository;
import io.github.logos_api.repository.ApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API 키 발급, 검증 및 로그 관리를 담당하는 서비스 클래스입니다.
 *
 * 주요 기능으로는 하루 발급 제한 확인을 통한 키 갱신, 클라이언트 IP 추출,
 * 그리고 오래된 발급 로그의 정기적인 삭제가 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyLogRepository apiKeyLogRepository;

    /**
     * 지정된 IP에 대해 새로운 API 키를 발급하거나 갱신합니다.
     * 하루 최대 10회까지만 발급이 가능하며, 기존에 해당 IP로 발급된 키는 삭제됩니다.
     *
     * @param clientIp 키를 발급받을 클라이언트의 IP 주소
     * @return 생성된 UUID 기반의 새로운 API 키 문자열
     * @throws RuntimeException 하루 발급 제한(10회)을 초과한 경우 발생
     */
    @Transactional
    public String renewApiKey(String clientIp){

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayCount = apiKeyLogRepository.countByIssuedIpAndCreatedAtAfter(clientIp, startOfToday);

        if(todayCount >= 10){
            log.warn("[Limit Exceeded] IP: {} has already issued {} keys today.", clientIp, todayCount);
            throw new RuntimeException("하루 발급 제한(10회)을 초과했습니다. 내일 다시 시도해주세요.");
        }

        apiKeyLogRepository.save(new ApiKeyLog(clientIp));

        log.info("Deleting existing keys for IP: {} " ,clientIp);
        apiKeyRepository.deleteByIssuedIp(clientIp);

        String newKey = UUID.randomUUID().toString().replace("-", "");

        ApiKey apiKey = new ApiKey();
        apiKey.setApiKey(newKey);
        apiKey.setIssuedIp(clientIp);
        apiKeyRepository.save(apiKey);

        return newKey;
    }

    /**
     * HTTP 요청 헤더 또는 원격 주소로부터 클라이언트의 실제 IP 주소를 추출합니다.
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    public String getClientIp(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 생성된 지 30일이 지난 API 키 발급 로그를 데이터베이스에서 삭제합니다.
     * 매일 새벽 3시에 스케줄링에 의해 자동으로 실행됩니다.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldApiLogs(){
        LocalDateTime targetDate = LocalDateTime.now().minusDays(30);
        log.info("[Cleanup] Starting API Key logs cleanup. Target date: before {}", targetDate);

        apiKeyLogRepository.deleteByCreatedAtBefore(targetDate);
        log.info("[Cleanup] API Key logs cleanup finished.");
    }
}