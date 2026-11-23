package io.github.logos_api.common.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

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

    private static final int IPV6_GROUPS = 8;
    private static final int IPV6_PREFIX_GROUPS = 4;

    /**
     * 인스턴스를 만들지 않는 유틸리티 클래스입니다.
     */
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

    /**
     * 한 클라이언트로 셀 단위의 키를 정합니다. IPv4와 알 수 없는 형식은 그대로 쓰고, IPv6는 앞 64비트만 남깁니다.
     * 한 사용자가 같은 /64 안에서 주소를 마음대로 바꿀 수 있어, 주소마다 세면 속도 제한과 발급 한도를 쉽게 피하기 때문입니다.
     *
     * @param ip 클라이언트 IP 주소
     * @return 속도 제한 버킷과 키 발급 한도를 셀 때 쓸 키
     */
    public static String groupKey(String ip){
        if (ip == null || !ip.contains(":") || ip.contains(".")) {
            return ip;
        }

        String address = ip.contains("%") ? ip.substring(0, ip.indexOf('%')) : ip;
        String[] halves = address.split("::", -1);
        if (halves.length > 2) {
            return ip;
        }

        String[] head = halves[0].isEmpty() ? new String[0] : halves[0].split(":");
        String[] tail = halves.length == 2 && !halves[1].isEmpty() ? halves[1].split(":") : new String[0];
        boolean compressed = halves.length == 2;
        if (compressed ? head.length + tail.length >= IPV6_GROUPS : head.length != IPV6_GROUPS) {
            return ip;
        }

        String[] groups = new String[IPV6_GROUPS];
        Arrays.fill(groups, "0");
        System.arraycopy(head, 0, groups, 0, head.length);
        System.arraycopy(tail, 0, groups, IPV6_GROUPS - tail.length, tail.length);

        try {
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < IPV6_PREFIX_GROUPS; i++) {
                key.append(Integer.toHexString(Integer.parseInt(groups[i], 16))).append(':');
            }
            return key.append(":/64").toString();
        } catch (NumberFormatException e) {
            return ip;
        }
    }
}