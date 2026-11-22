package io.github.logos_api.controller;

import io.github.logos_api.dto.ErrorResponseDTO;
import io.github.logos_api.dto.LogosResponseDTO;
import io.github.logos_api.service.LogosService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

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
     * 성경 구절을 반환합니다. {@code ref}를 주면 그 참조의 구절을, 주지 않으면 무작위 구절을 돌려줍니다.
     * <p>
     * {@code ref}는 "요3:16", "요한복음 3:16-18", "요3"처럼 쓰고 약어와 정식 이름을 모두 받습니다.
     * 절을 지정하지 않으면 그 장 전체를 배열로 돌려줍니다.
     * 무작위 구절은 매번 달라지므로 3초만 캐싱하고, 참조 조회는 내용이 고정이라 하루 캐싱합니다.
     * </p>
     *
     * @param ref 참조 문자열. 없으면 무작위 구절
     * @return 구절 정보. 참조 형식이 틀리면 400, 해당 구절이 없으면 404
     */
    @Operation(summary = "구절 조회", description = "ref를 주면 그 참조의 구절을, 주지 않으면 무작위 구절을 돌려줍니다.")
    @GetMapping("/verse")
    public ResponseEntity<?> verse(@RequestParam(required = false) String ref){
        if (ref == null || ref.isBlank()) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(3)).cachePublic())
                    .body(logosService.getRandomVerse());
        }

        if (!logosService.isReadableReference(ref)) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of(HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "참조 형식을 읽을 수 없습니다. 예: 요3:16, 요한복음 3:16-18, 요3"));
        }

        List<LogosResponseDTO> verses = logosService.findByReference(ref);
        if (verses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponseDTO.of(HttpStatus.NOT_FOUND.getReasonPhrase(),
                            "해당 참조의 구절이 없습니다. 책 이름과 장 · 절 번호를 확인하세요."));
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(verses.size() == 1 ? verses.get(0) : verses);
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
    @Operation(summary = "구절 검색", description = "본문에 검색어가 들어간 구절을 정경 순서로 찾습니다.")
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q,
                                    @RequestParam(required = false) String book,
                                    @RequestParam(defaultValue = "20") int limit,
                                    @RequestParam(defaultValue = "0") int offset){
        String query = q == null ? "" : q.trim();

        if (query.length() < MIN_QUERY_LENGTH || query.length() > MAX_QUERY_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of(HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "검색어는 " + MIN_QUERY_LENGTH + "자 이상 " + MAX_QUERY_LENGTH + "자 이하여야 합니다."));
        }

        if (book != null && !book.isBlank() && !logosService.isRegisteredBook(book)) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of(HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "등록되지 않은 책 이름입니다. 책 목록은 /api/books 에서 확인하세요."));
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
    @Operation(summary = "오늘의 말씀", description = "날짜 기준으로 고정된 구절입니다. 같은 날에는 누가 호출해도 같습니다.")
    @GetMapping("/verse/today")
    public ResponseEntity<LogosResponseDTO> today(){
        return ResponseEntity.ok()
                .body(logosService.getTodayVerse());
    }
}