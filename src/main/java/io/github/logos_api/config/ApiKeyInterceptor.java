package io.github.logos_api.config;

import io.github.logos_api.repository.ApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String referer = request.getHeader("Referer");
        String apikey = request.getHeader("X-API-KEY");

        if(referer != null && referer.contains("logos-api.com")){
            return true;
        }

        if(apikey == null || apikey.isBlank()){
            log.warn("[Unauthorized] API Key is missing. URI: {}", request.getRequestURI());
            return fail(response, "API Key is missing", HttpStatus.UNAUTHORIZED);
        }

        if(!apiKeyRepository.existsByApiKey(apikey)){
            log.warn("[Unauthorized] Invalid API Key: {}. URI: {}", apikey, request.getRequestURI());
            return fail(response, "Invalid API Key", HttpStatus.UNAUTHORIZED);
        }

        return true;
    }

    private boolean fail(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");

        return false;
    }
}