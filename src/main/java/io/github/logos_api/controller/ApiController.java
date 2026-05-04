package io.github.logos_api.controller;

import io.github.logos_api.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API 키 생성 및 관리를 위한 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/key")
@RequiredArgsConstructor
public class ApiController {

    private final ApiKeyService apiKeyService;

    /**
     * 사용자의 IP 주소를 기반으로 새로운 API 키를 생성하여 반환합니다.
     * 하루 발급 제한 횟수를 초과한 경우 429 에러와 함께 메시지를 응답합니다.
     *
     * @param request 클라이언트 IP 추출을 위한 HTTP 요청 객체
     * @return 발급된 신규 API 키 또는 에러 메시지
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateKey(HttpServletRequest request){
        String clientIp = apiKeyService.getClientIp(request);

        try{
            String newKey = apiKeyService.renewApiKey(clientIp);
            return ResponseEntity.ok(newKey);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}