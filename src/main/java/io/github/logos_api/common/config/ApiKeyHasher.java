package io.github.logos_api.common.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * API 키를 저장용 해시로 바꾸는 유틸리티입니다.
 * <p>
 * 키를 그대로 저장하면 데이터베이스 덤프나 백업이 새는 순간 모든 키를 바로 쓸 수 있습니다.
 * 저장은 해시로만 하고, 원문은 발급 응답에서 한 번만 보여 줍니다.
 * </p>
 * <p>
 * 비밀번호와 달리 키는 UUID라서 경우의 수가 충분히 많습니다.
 * 무차별 대입이 통하지 않으므로 느린 해시(bcrypt 등) 대신 SHA-256을 씁니다.
 * </p>
 */
public class ApiKeyHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 인스턴스를 만들지 않는 유틸리티 클래스입니다.
     */
    private ApiKeyHasher(){
    }

    /**
     * API 키를 SHA-256 16진수 문자열로 바꿉니다.
     *
     * @param apiKey 원문 키
     * @return 64자 16진수 해시
     * @throws IllegalStateException 실행 환경에 SHA-256이 없는 경우
     */
    public static String hash(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 알고리즘을 쓸 수 없습니다", e);
        }
    }
}
