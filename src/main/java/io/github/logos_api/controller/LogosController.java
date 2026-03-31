package io.github.logos_api.controller;

import io.github.logos_api.dto.LogosResponseDTO;
import io.github.logos_api.service.LogosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LogosController {

    private final LogosService logosService;

    @GetMapping("/verse")
    public ResponseEntity<LogosResponseDTO> random(){
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePublic())
                .body(logosService.getRandomVerse());
    }
}