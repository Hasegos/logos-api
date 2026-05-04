package io.github.logos_api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클라이언트 IP별 요청 속도 제한(Rate Limiting)을 관리하는 서비스입니다.
 * Bucket4j 라이브러리를 사용하여 처리율 제한 로직을 구현합니다.
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * IP별로 적용될 새로운 처리율 제한 버킷을 생성합니다.
     * 1분(60초)마다 60개의 토큰이 충전되는 대역폭(Bandwidth)을 설정합니다.
     *
     * @return 1분당 60회 요청이 가능한 Bucket 객체
     */
    private Bucket createNewBucket(){
        Bandwidth limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit).build();
    }

    /**
     * 특정 IP 주소에 해당하는 버킷을 조회하거나 없으면 새로 생성하여 반환합니다.
     * 기본 설정은 분당 60개의 토큰이 충전되는 방식입니다.
     *
     * @param ip 클라이언트 IP 주소
     * @return 해당 IP 전용 버킷 객체
     */
    public Bucket resolveBucket(String ip){
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }
}