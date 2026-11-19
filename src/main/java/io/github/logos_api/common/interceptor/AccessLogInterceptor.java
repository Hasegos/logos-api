package io.github.logos_api.common.interceptor;

import io.github.logos_api.common.config.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 모든 요청의 접근 로그를 남기는 인터셉터입니다.
 * `/api/**` 호출은 API 접근 로그로, 그 외 페이지 방문은 페이지 접근 로그로 나누어 기록합니다.
 */
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger apiLog = LoggerFactory.getLogger("io.github.logos_api.access.api");
    private static final Logger pageLog = LoggerFactory.getLogger("io.github.logos_api.access.page");
    private static final String API_PREFIX = "/api/";
    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);
    private static final String START_TIME_ATTR = "accessLog.startTime";
    private static final int MAX_UA_LENGTH = 200;
    private static final int REQUEST_ID_LENGTH = 8;
    private static final long SLOW_REQUEST_MS = 1000;

    /**
     * 요청 처리 시작 시각을 기록하고 이 요청의 IP와 요청 ID를 MDC에 넣습니다.
     * MDC에 넣어 두면 이 요청이 남기는 모든 로그 줄에 IP와 요청 ID가 자동으로 붙어,
     * 접근 로그와 에러 로그를 요청 ID 하나로 묶어 볼 수 있습니다.
     * 로깅 전용 인터셉터이므로 항상 true를 반환합니다.
     *
     * @return 항상 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        MDC.put("clientIp", ClientIpResolver.resolve(request));
        MDC.put("reqId", UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH));
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

            logFor(request).info("ip={} method={} uri={} status={} auth={} tookMs={} ua={}",
                    ClientIpResolver.resolve(request),
                    request.getMethod(),
                    sanitize(request.getRequestURI()),
                    response.getStatus(),
                    resolveAuthMethod(request),
                    tookMs,
                    sanitizeUserAgent(request.getHeader("User-Agent")));

            warnIfSlow(request, tookMs);
        } catch (Exception e) {
            pageLog.warn("접근 로그 기록 실패: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    /**
     * 처리 시간이 {@value #SLOW_REQUEST_MS}ms를 넘으면 경고를 남깁니다.
     * 접근 로그에 묻히지 않게 error.log로 보내, 성능 저하를 당일에 알 수 있게 합니다.
     *
     * @param request HTTP 요청 객체
     * @param tookMs  처리에 걸린 시간 (ms)
     */
    private void warnIfSlow(HttpServletRequest request, long tookMs) {
        if (tookMs >= SLOW_REQUEST_MS) {
            log.warn("[느린 요청] uri={} tookMs={}", sanitize(request.getRequestURI()), tookMs);
        }
    }

    /**
     * 요청 경로에 따라 기록할 로거를 고릅니다. API 호출과 페이지 방문을 각각 다른 파일에 남기기 위해서입니다.
     *
     * @param request HTTP 요청 객체
     * @return `/api/`로 시작하면 API 로거, 그 외에는 페이지 로거
     */
    private Logger logFor(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX) ? apiLog : pageLog;
    }

    /**
     * 로그 위조(개행 삽입)를 막기 위해 제어문자를 치환합니다.
     *
     * @param value 로그에 남길 값
     * @return 개행과 탭이 밑줄로 바뀐 값
     */
    private String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }

    /**
     * 요청이 어떤 방식으로 인증되었는지 식별합니다. API 키 원문은 로그에 남기지 않습니다.
     *
     * @return "api-key" | "none" 중 하나
     */
    private String resolveAuthMethod(HttpServletRequest request) {
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