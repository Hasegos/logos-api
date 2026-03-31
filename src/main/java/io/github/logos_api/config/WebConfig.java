package io.github.logos_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       registry.addInterceptor(apiKeyInterceptor)
               .addPathPatterns("/api/**")
               .excludePathPatterns("/api/verse")
               .order(1);

       registry.addInterceptor(rateLimitInterceptor)
               .addPathPatterns("/api/**")
               .excludePathPatterns("/api/key/generate")
               .order(2);
    }
}