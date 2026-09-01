package io.github.logos_api.controller;

import io.github.logos_api.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * API 키 생성 및 관리를 위한 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/key")
@RequiredArgsConstructor
public class ApiController {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://logos-api.com",
            "https://www.logos-api.com"
    );

    private final ApiKeyService apiKeyService;

    /**
     * 사용자의 IP 주소를 기반으로 새로운 API 키를 생성하여 반환합니다.
     * 하루 발급 제한 횟수를 초과한 경우 429 에러와 함께 메시지를 응답합니다.
     * <p>
     * 이 엔드포인트는 홈페이지 버튼을 통한 호출만 의도하므로, 외부 사이트가 숨겨진 폼으로
     * 방문자 모르게 키를 재발급시키는 CSRF성 악용을 막기 위해 Origin 헤더를 검증합니다.
     * curl 등 브라우저를 거치지 않는 직접 호출은 Origin 헤더 자체가 없으므로 검증 대상에서 제외됩니다.
     * </p>
     *
     * @param request 클라이언트 IP 추출 및 Origin 검증을 위한 HTTP 요청 객체
     * @return 발급된 신규 API 키 또는 에러 메시지
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateKey(HttpServletRequest request){
        if (!isSafeOrigin(request)) {
            log.warn("[CSRF 의심] 허용되지 않은 Origin에서 키 발급 요청 - Origin: {}", request.getHeader("Origin"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden"));
        }

        String clientIp = apiKeyService.getClientIp(request);

        try{
            String newKey = apiKeyService.renewApiKey(clientIp);
            return ResponseEntity.ok(newKey);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 요청의 Origin 헤더가 우리 도메인이거나, 브라우저를 거치지 않은 직접 호출(Origin 헤더 없음)인지 확인합니다.
     *
     * @param request HTTP 요청 객체
     * @return 안전한 출처로 판단되면 true
     */
    private boolean isSafeOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }
        return ALLOWED_ORIGINS.contains(origin);
    }
}