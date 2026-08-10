package io.github.logos_api.common.interceptor;

import io.github.logos_api.common.config.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger accessLog = LoggerFactory.getLogger("io.github.logos_api.access");
    private static final String START_TIME_ATTR = "accessLog.startTime";
    private static final int MAX_UA_LENGTH = 200;

    /**
     * 요청 처리 시작 시각을 기록합니다. 로깅 전용 인터셉터이므로 항상 true를 반환합니다.
     *
     * @return 항상 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    /**
     * 요청 처리가 완전히 끝난 뒤(다른 인터셉터의 거부 여부와 무관하게) 접근 로그를 남깁니다.
     * 로그 기록 중 예외가 발생해도 요청 처리 흐름에는 영향을 주지 않습니다.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Long start = (Long) request.getAttribute(START_TIME_ATTR);
            long tookMs = (start != null) ? System.currentTimeMillis() - start : -1;

            accessLog.info("ip={} method={} uri={} status={} auth={} tookMs={} ua={}",
                    ClientIpResolver.resolve(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    resolveAuthMethod(request),
                    tookMs,
                    sanitizeUserAgent(request.getHeader("User-Agent")));
        } catch (Exception e) {
            accessLog.warn("접근 로그 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 요청이 어떤 방식으로 인증되었는지 식별합니다. API 키 원문은 로그에 남기지 않습니다.
     *
     * @return "referer" | "api-key" | "none" 중 하나
     */
    private String resolveAuthMethod(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("logos-api.com")) {
            return "referer";
        }
        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            return "api-key";
        }
        return "none";
    }

    /**
     * User-Agent 값에서 로그 위조에 악용될 수 있는 개행/탭 문자를 치환하고 길이를 제한합니다.
     *
     * @param ua 원본 User-Agent 헤더 값
     * @return 정제된 User-Agent 문자열
     */
    private String sanitizeUserAgent(String ua) {
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String cleaned = ua.replaceAll("[\r\n\t]", "_");
        return cleaned.substring(0, Math.min(cleaned.length(), MAX_UA_LENGTH));
    }
}