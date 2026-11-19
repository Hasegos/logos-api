package io.github.logos_api.common.interceptor;

import io.github.logos_api.common.config.ClientIpResolver;
import io.github.logos_api.common.config.LogSanitizer;
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
 * `/api/**` 호출은 API 접근 로그로, 그 외 페이지 방문은 사람 · 봇 · 스냅샷 굽기로 나누어 기록합니다.
 */
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger apiLog = LoggerFactory.getLogger("io.github.logos_api.access.api");
    private static final Logger pageHumanLog = LoggerFactory.getLogger("io.github.logos_api.access.page.human");
    private static final Logger pageBotLog = LoggerFactory.getLogger("io.github.logos_api.access.page.bot");
    private static final Logger pageSelfLog = LoggerFactory.getLogger("io.github.logos_api.access.page.self");
    private static final String API_PREFIX = "/api/";
    private static final String SNAPSHOT_UA = "logos-snapshot";
    private static final String[] BOT_UA_MARKERS = {"bot", "crawler", "spider", "yeti", "slurp"};
    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);
    private static final String START_TIME_ATTR = "accessLog.startTime";
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

            logFor(request, response).info("ip={} method={} uri={} status={} auth={} tookMs={} ua={}",
                    ClientIpResolver.resolve(request),
                    request.getMethod(),
                    LogSanitizer.sanitize(request.getRequestURI()),
                    response.getStatus(),
                    resolveAuthMethod(request),
                    tookMs,
                    LogSanitizer.userAgent(request));

            warnIfSlow(request, tookMs);
        } catch (Exception e) {
            log.warn("접근 로그 기록 실패: {}", e.getMessage());
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
            log.warn("[느린 요청] uri={} tookMs={}", LogSanitizer.sanitize(request.getRequestURI()), tookMs);
        }
    }

    /**
     * 요청을 기록할 로거를 고릅니다. 파일이 갈려야 사람의 방문만 따로 셀 수 있습니다.
     * <p>
     * API 호출은 api-access, 스냅샷 굽기는 page-self, 봇은 page-bot, 나머지는 page-human으로 보냅니다.
     * 없는 주소(404)를 받은 요청도 page-bot으로 보냅니다. 취약점을 긁는 스캐너는 대부분 UA를 숨기고
     * 없는 주소만 때리므로, 사람의 방문 기록을 깨끗하게 유지하려면 여기서 걸러야 합니다.
     * 그 외에 판정이 애매하면 page-human으로 보냅니다. 사람 기록이 봇 파일로 새면 찾기 어렵기 때문입니다.
     * </p>
     *
     * @param request  HTTP 요청 객체
     * @param response 상태 코드를 확인할 응답 객체
     * @return 이 요청을 남길 로거
     */
    private Logger logFor(HttpServletRequest request, HttpServletResponse response) {
        if (request.getRequestURI().startsWith(API_PREFIX)) {
            return apiLog;
        }

        if (response.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            return pageBotLog;
        }

        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return pageHumanLog;
        }

        String lowerUa = ua.toLowerCase();
        if (lowerUa.contains(SNAPSHOT_UA)) {
            return pageSelfLog;
        }
        for (String marker : BOT_UA_MARKERS) {
            if (lowerUa.contains(marker)) {
                return pageBotLog;
            }
        }
        return pageHumanLog;
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

}