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
    private static final int SECRET_PREFIX_LENGTH = 8;

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
     * 비밀값을 앞부분만 남기고 가립니다.
     * <p>
     * 거부된 키라도 그대로 남기면, 같은 키가 다른 서버에서는 유효하거나 한 글자만 틀린 값일 수 있습니다.
     * 로그는 보관 기간이 길고 공유되기 쉬우므로 앞 {@value #SECRET_PREFIX_LENGTH}자와 길이만 남깁니다.
     * </p>
     *
     * @param secret 가릴 값
     * @return 예: "941f4f5c…(32자)"
     */
    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return "(없음)";
        }
        String cleaned = sanitize(secret);
        if (cleaned.length() <= SECRET_PREFIX_LENGTH) {
            return "…(" + cleaned.length() + "자)";
        }
        return cleaned.substring(0, SECRET_PREFIX_LENGTH) + "…(" + cleaned.length() + "자)";
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
