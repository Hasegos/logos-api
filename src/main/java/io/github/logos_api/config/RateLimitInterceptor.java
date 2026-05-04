package io.github.logos_api.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.logos_api.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 클라이언트 IP 주소를 기반으로 API 요청 횟수를 제한하는 인터셉터입니다.
 * Bucket4j를 사용하여 분당 요청 제한을 초과한 경우 429(Too Many Requests) 에러를 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    /**
     * 클라이언트의 요청이 허용된 처리율 이내인지 확인합니다.
     * 통과 시 남은 토큰 정보를 헤더에 포함하며, 초과 시 재시도 가능 시간을 응답합니다.
     *
     * @return 요청 허용 시 true, 제한 초과 시 false 반환
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        MDC.put("clientIp", ip);
        Bucket bucket = rateLimitService.resolveBucket(ip);

        log.info("[API Request] Method: {}, URI: {}, IP: {}", request.getMethod(), request.getRequestURI(), ip);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if(probe.isConsumed()){

            log.debug("IP {} has {} tokens remaining.", ip, probe.getRemainingTokens());

            response.addHeader("X-Rate-Limit-limit", "60");
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }
        else{
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("[Rate Limit Exceeded] IP: {} is blocked. Retry after {} seconds.", ip, waitForRefill);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.addHeader("X-RateLimit-Retry-After", String.valueOf(waitForRefill));
            response.getWriter().write("{\"error\": \"Too Many Requests\",\"retry_after_seconds\":"+waitForRefill +"}");

            MDC.clear();
            return false;
        }
    }

    /**
     * 요청 처리가 완료된 후 실행되는 후처리 메서드입니다.
     * 현재 쓰레드에 저장된 로그 관리용 MDC(Mapped Diagnostic Context) 정보를 삭제하여 메모리 누수를 방지합니다.
     *
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param handler  실행된 핸들러 객체
     * @param ex       발생한 예외 (없을 경우 null)
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.clear();
    }

    /**
     * HTTP 요청 정보에서 클라이언트의 실제 IP 주소를 식별합니다.
     * 프록시 서버나 로드 밸런서를 거치는 경우를 대비하여 'X-Forwarded-For' 헤더를 우선적으로 확인합니다.
     *
     * @param request HTTP 요청 객체
     * @return 식별된 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}