package io.github.logos_api.common.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 로그에 남길 외부 입력값을 다듬는 공통 유틸리티입니다.
 * <p>
 * 요청 경로나 헤더 값에 개행이 들어 있으면 로그 한 줄을 여러 줄로 쪼개 다른 기록처럼 꾸밀 수 있으므로,
 * 제어문자를 밑줄로 바꾸고 User-Agent는 길이를 제한합니다.
 * </p>
 */
public class LogSanitizer {

    private static final int MAX_UA_LENGTH = 200;

    private LogSanitizer(){
    }

    /**
     * 개행과 탭을 밑줄로 바꿉니다.
     *
     * @param value 로그에 남길 값
     * @return 정제된 값 (입력이 null이면 null)
     */
    public static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }

    /**
     * 요청의 User-Agent를 정제하고 {@value #MAX_UA_LENGTH}자로 자릅니다.
     *
     * @param request HTTP 요청 객체
     * @return 정제된 User-Agent (없으면 "unknown")
     */
    public static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String cleaned = sanitize(ua);
        return cleaned.substring(0, Math.min(cleaned.length(), MAX_UA_LENGTH));
    }
}
