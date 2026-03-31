package io.github.logos_api.controller;

import io.github.logos_api.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/key")
@RequiredArgsConstructor
public class ApiController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateKey(HttpServletRequest request){
        String clientIp = apiKeyService.getClientIp(request);

        try{
            String newKey = apiKeyService.renewApiKey(clientIp);
            return ResponseEntity.ok(newKey);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}