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

/**
 * 성경 구절 조회를 담당하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://logos-api.com/")
@RequiredArgsConstructor
public class LogosController {

    private final LogosService logosService;

    /**
     * 무작위 성경 구절을 하나 반환합니다.
     * 브라우저 캐시 설정을 통해 3초 동안의 캐싱을 권장합니다.
     *
     * @return 랜덤 성경 구절 정보
     */
    @GetMapping("/verse")
    public ResponseEntity<LogosResponseDTO> random(){
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(3)).cachePublic())
                .body(logosService.getRandomVerse());
    }
}