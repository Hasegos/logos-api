package io.github.logos_api.config;

import io.github.logos_api.repository.ApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * API 요청 시 유효한 API Key가 포함되어 있는지 검증하는 인터셉터입니다.
 * 특정 도메인(logos-api.com)에서의 요청은 통과시키며, 그 외 요청은 헤더의 'X-API-KEY' 값을 검사합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * API Key의 존재 여부와 데이터베이스 내 유효성을 확인합니다.
     *
     * @return 유효한 요청이거나 특정 도메인 요청인 경우 true, 그렇지 않으면 false 반환 및 401 에러 응답
     * @throws Exception 인증 실패 시 응답 작성 과정에서 발생할 수 있는 예외
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String referer = request.getHeader("Referer");
        String apikey = request.getHeader("X-API-KEY");

        if(referer != null && referer.contains("logos-api.com")){
            return true;
        }

        if(apikey == null || apikey.isBlank()){
            log.warn("[Unauthorized] API Key is missing. URI: {}", request.getRequestURI());
            return fail(response, "API Key is missing", HttpStatus.UNAUTHORIZED);
        }

        if(!apiKeyRepository.existsByApiKey(apikey)){
            log.warn("[Unauthorized] Invalid API Key: {}. URI: {}", apikey, request.getRequestURI());
            return fail(response, "Invalid API Key", HttpStatus.UNAUTHORIZED);
        }

        return true;
    }

    /**
     * API 인증 실패 시 클라이언트에게 에러 응답을 전송합니다.
     * 응답 상태 코드와 JSON 형식의 에러 메시지를 설정한 후 인터셉터 실행을 중단합니다.
     *
     * @param response HTTP 응답 객체
     * @param message JSON 에러 응답에 포함될 메시지
     * @param status 응답에 설정할 HTTP 상태 코드 (예: 401 Unauthorized)
     * @return 항상 false를 반환하여 핸들러 실행을 차단함
     * @throws IOException 응답 스트림 출력 중 발생할 수 있는 예외
     */
    private boolean fail(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");

        return false;
    }
}