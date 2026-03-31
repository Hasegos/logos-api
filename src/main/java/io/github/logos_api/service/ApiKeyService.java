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

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyLogRepository apiKeyLogRepository;

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

    public String getClientIp(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldApiLogs(){
        LocalDateTime targetDate = LocalDateTime.now().minusDays(30);
        log.info("[Cleanup] Starting API Key logs cleanup. Target date: before {}", targetDate);

        apiKeyLogRepository.deleteByCreatedAtBefore(targetDate);
        log.info("[Cleanup] API Key logs cleanup finished.");
    }
}