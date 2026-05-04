package io.github.logos_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 관련 인터셉터 및 설정을 담당하는 구성 클래스입니다.
 * API 키 검증 인터셉터와 속도 제한 인터셉터를 등록하고 적용 경로를 설정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 애플리케이션 인터셉터 체인을 설정합니다.
     * 1순위: API 키 검증 (단, 키 발급 경로는 제외)
     * 2순위: 속도 제한 적용 (단, 구절 조회 경로는 제외)
     *
     * @param registry 인터셉터 등록 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       registry.addInterceptor(apiKeyInterceptor)
               .addPathPatterns("/api/**")
               .excludePathPatterns("/api/key/generate")
               .order(1);

       registry.addInterceptor(rateLimitInterceptor)
               .addPathPatterns("/api/**")
               .excludePathPatterns("/api/verse")
               .order(2);
    }
}