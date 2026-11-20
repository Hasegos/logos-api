package io.github.logos_api.controller;

import io.github.logos_api.service.LogosService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 책/장/절 단위 구조적 탐색을 위한 REST 컨트롤러입니다.
 * <p>
 * 없는 책이나 장을 빈 배열과 200으로 돌려주면 호출한 쪽은 이름을 잘못 쓴 것인지 본문이 없는 것인지
 * 구분하지 못하므로, 등록 여부를 먼저 묻고 404와 안내를 돌려줍니다. 판정은 {@link LogosService}가 합니다.
 * </p>
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BibleController {

    private final LogosService logosService;

    /**
     * 데이터가 존재하는 책 목록을 성경 정경 순서로 반환합니다.
     *
     * @return 책 이름 목록
     */
    @Operation(summary = "책 목록", description = "데이터가 등록된 책을 정경 순서로 돌려줍니다.")
    @GetMapping
    public List<String> books() {
        return logosService.getAvailableBooks();
    }

    /**
     * 지정한 책에 존재하는 장 번호 목록을 반환합니다.
     *
     * @param book 책 이름
     * @return 장 번호 목록. 등록되지 않은 책이면 404
     */
    @Operation(summary = "장 목록", description = "지정한 책에 있는 장 번호를 돌려줍니다.")
    @GetMapping("/{book}/chapters")
    public ResponseEntity<?> chapters(@PathVariable String book){
        if (!logosService.isRegisteredBook(book)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(bookNotFound());
        }
        return ResponseEntity.ok(logosService.getChapters(book));
    }

    /**
     * 지정한 책의 특정 장에 속한 전체 절을 반환합니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return 절 목록 (절 번호 + 본문). 등록되지 않은 책이나 없는 장이면 404
     */
    @Operation(summary = "장 본문", description = "지정한 장의 절 번호와 본문을 돌려줍니다.")
    @GetMapping("/{book}/chapters/{chapter}")
    public ResponseEntity<?> verses(@PathVariable String book,
                                    @PathVariable int chapter){
        if (!logosService.isRegisteredBook(book)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(bookNotFound());
        }
        if (!logosService.isRegisteredChapter(book, chapter)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found",
                            "message", "그 책에 없는 장입니다. 장 번호는 /api/books/{책}/chapters 에서 확인하세요."));
        }
        return ResponseEntity.ok(logosService.getChapterVerses(book, chapter));
    }

    /**
     * 등록되지 않은 책 이름일 때 돌려줄 응답 본문을 만듭니다.
     *
     * @return 오류 종류와 다음에 볼 곳을 담은 본문
     */
    private Map<String, String> bookNotFound() {
        return Map.of("error", "Not Found",
                "message", "등록되지 않은 책 이름입니다. 책 목록은 /api/books 에서 확인하세요.");
    }
}
