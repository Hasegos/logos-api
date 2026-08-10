package io.github.logos_api.common.config;

import io.github.logos_api.common.interceptor.AccessLogInterceptor;
import io.github.logos_api.common.interceptor.HostValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 관련 인터셉터 및 설정을 담당하는 구성 클래스입니다.
 * Host 검증, 접근 로그, API 키 검증, 속도 제한 인터셉터를 등록하고 적용 경로를 설정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final HostValidationInterceptor hostValidationInterceptor;
    private final AccessLogInterceptor accessLogInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 애플리케이션 인터셉터 체인을 설정합니다.
     * 0순위: Host 헤더 검증 (전체 경로 - 위조된 Host는 가장 먼저 차단)
     * 1순위: 접근 로그 기록 (제외 경로 없음 - 뒤 인터셉터가 거부해도 항상 기록되어야 하므로)
     * 2순위: API 키 검증 (단, 키 발급 경로는 제외)
     * 3순위: 속도 제한 적용 (단, 구절 조회 경로는 제외)
     *
     * @param registry 인터셉터 등록 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hostValidationInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/key/generate")
                .order(2);

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/verse")
                .order(3);
    }
}