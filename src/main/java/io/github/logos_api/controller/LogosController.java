package io.github.logos_api.controller;

import io.github.logos_api.dto.LogosResponseDTO;
import io.github.logos_api.service.LogosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 성경 구절 조회를 담당하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://logos-api.com")
@RequiredArgsConstructor
public class LogosController {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_LIMIT = 50;

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

    /**
     * 본문에 검색어가 포함된 구절을 정경 순서로 찾아 반환합니다.
     * limit/offset은 허용 범위를 벗어나면 가장 가까운 값으로 맞춥니다. (limit 1~50, offset 0 이상)
     *
     * @param q      검색어. 앞뒤 공백을 제외하고 2자 이상 100자 이하여야 한다
     * @param book   특정 책으로 좁힐 때의 책 이름. 등록되지 않은 이름이면 400
     * @param limit  한 번에 돌려줄 건수 (기본 20, 최대 50)
     * @param offset 건너뛸 건수 (기본 0)
     * @return 검색 결과 또는 400 에러 메시지
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q,
                                    @RequestParam(required = false) String book,
                                    @RequestParam(defaultValue = "20") int limit,
                                    @RequestParam(defaultValue = "0") int offset){
        String query = q == null ? "" : q.trim();

        if (query.length() < MIN_QUERY_LENGTH || query.length() > MAX_QUERY_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request",
                            "message", "검색어는 " + MIN_QUERY_LENGTH + "자 이상 " + MAX_QUERY_LENGTH + "자 이하여야 합니다."));
        }

        if (book != null && !book.isBlank() && !logosService.getAvailableBooks().contains(book)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", "등록되지 않은 책 이름입니다."));
        }

        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int safeOffset = Math.max(offset, 0);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(logosService.search(query, book, safeLimit, safeOffset));
    }

    /**
     * 매일 자정 기준으로 고정되는 오늘의 말씀을 반환합니다.
     * 같은 날짜에 호출하면 누가 호출하든 항상 동일한 구절을 반환합니다.
     *
     * @return 오늘의 말씀 정보
     */
    @GetMapping("/verse/today")
    public ResponseEntity<LogosResponseDTO> today(){
        return ResponseEntity.ok()
                .body(logosService.getTodayVerse());
    }
}