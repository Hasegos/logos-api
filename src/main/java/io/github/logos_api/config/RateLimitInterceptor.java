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

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.clear();
    }

    private String getClientIp(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}