package io.github.logos_api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클라이언트 IP별 요청 속도 제한(Rate Limiting)을 관리하는 서비스입니다.
 * Bucket4j 라이브러리를 사용하여 처리율 제한 로직을 구현합니다.
 * <p>
 * IPv6 주소는 한 사용자가 같은 /64 안에서 주소를 마음대로 바꿀 수 있어 주소마다 세면 한도를 쉽게 피하므로,
 * 앞 64비트가 같은 주소를 하나의 클라이언트로 묶습니다. 버킷이 계속 쌓이지 않도록 쉬고 있는 버킷은 주기적으로 지웁니다.
 * </p>
 */
@Service
public class RateLimitService {

    private static final int LIMIT_PER_MINUTE = 60;
    private static final int IPV6_GROUPS = 8;
    private static final int IPV6_PREFIX_GROUPS = 4;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * IP별로 적용될 새로운 처리율 제한 버킷을 생성합니다.
     * 1분(60초)마다 60개의 토큰이 충전되는 대역폭(Bandwidth)을 설정합니다.
     *
     * @return 1분당 60회 요청이 가능한 Bucket 객체
     */
    private Bucket createNewBucket(){
        Bandwidth limit = Bandwidth.classic(LIMIT_PER_MINUTE, Refill.intervally(LIMIT_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit).build();
    }

    /**
     * 특정 IP 주소에 해당하는 버킷을 조회하거나 없으면 새로 생성하여 반환합니다.
     * 기본 설정은 분당 60개의 토큰이 충전되는 방식이며, IPv6는 /64 단위로 하나의 버킷을 씁니다.
     *
     * @param ip 클라이언트 IP 주소
     * @return 해당 IP 전용 버킷 객체
     */
    public Bucket resolveBucket(String ip){
        return cache.computeIfAbsent(clientKey(ip), k -> createNewBucket());
    }

    /**
     * 토큰이 가득 찬 버킷을 지웁니다. 가득 찼다는 것은 한 주기 넘게 쓰이지 않았다는 뜻이라,
     * 지워도 다음 요청이 새 버킷(가득 찬 상태)으로 시작하는 것과 같아 한도가 느슨해지지 않습니다.
     */
    @Scheduled(fixedRate = 600_000)
    public void evictIdleBuckets(){
        cache.values().removeIf(bucket -> bucket.getAvailableTokens() >= LIMIT_PER_MINUTE);
    }

    /**
     * 속도 제한을 셀 단위를 정합니다. IPv4와 알 수 없는 형식은 그대로 쓰고, IPv6는 앞 64비트만 남깁니다.
     *
     * @param ip 클라이언트 IP 주소
     * @return 버킷을 찾을 때 쓸 키
     */
    private static String clientKey(String ip){
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
