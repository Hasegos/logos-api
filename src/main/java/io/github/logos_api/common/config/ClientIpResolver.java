package io.github.logos_api.common.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트의 실제 IP 주소를 추출하는 공통 유틸리티입니다.
 * <p>
 * 우선순위: {@code CF-Connecting-IP} → {@code X-Forwarded-For} → {@code RemoteAddr}.
 * {@code X-Forwarded-For}만으로는 Cloudflare 프록시 환경에서 실제 방문자가 아닌
 * Cloudflare 엣지 서버의 IP가 잡힐 수 있어({@code real_ip_module} 미설정 시), Cloudflare가
 * 직접 부여하는 {@code CF-Connecting-IP}를 최우선으로 사용합니다.
 * </p>
 */
public class ClientIpResolver {

    private ClientIpResolver(){
    }

    /**
     * 요청으로부터 클라이언트의 실제 IP 주소를 판별합니다.
     *
     * @param request HTTP 요청 객체
     * @return 판별된 클라이언트 IP 주소
     */
    public static String resolve(HttpServletRequest request){
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }

        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}