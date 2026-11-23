package io.github.logos_api.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Host 헤더를 검증하는 인터셉터.
 * <p>
 * 허용되지 않은 Host 헤더로 들어오는 요청을 차단하여
 * DNS Rebinding 공격을 방어한다.
 * 로컬 개발용 Host는 Cloudflare를 거치지 않은 요청에만 허용한다. 운영의 모든 요청은 Cloudflare가
 * {@code CF-Connecting-IP}를 붙이므로, 이 헤더가 있으면 로컬 Host로 위장한 요청으로 보고 막는다.
 * </p>
 */
@Slf4j
@Component
public class HostValidationInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_HOSTS = Set.of(
            "logos-api.com",
            "www.logos-api.com"
    );
    private static final String LOCAL_HOST = "localhost:8081";
    private static final String CLOUDFLARE_IP_HEADER = "CF-Connecting-IP";

    /**
     * 요청의 Host 헤더가 허용된 값인지 확인합니다.
     *
     * @return 허용된 Host이면 true, 아니면 400 응답을 보내고 false
     * @throws Exception 응답 작성 중 발생할 수 있는 예외
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String host = request.getHeader("host");

        if (!isAllowed(host, request)) {
            log.warn("허용되지 않은 Host 헤더 감지 - Host: {}", host);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        return true;
    }

    /**
     * 공개 Host이거나, Cloudflare를 거치지 않은 로컬 개발 Host인지 판정합니다.
     *
     * @param host    Host 헤더 값
     * @param request HTTP 요청 객체
     * @return 허용되면 true
     */
    private boolean isAllowed(String host, HttpServletRequest request) {
        if (host == null) {
            return false;
        }
        return PUBLIC_HOSTS.contains(host)
                || (LOCAL_HOST.equals(host) && request.getHeader(CLOUDFLARE_IP_HEADER) == null);
    }
}