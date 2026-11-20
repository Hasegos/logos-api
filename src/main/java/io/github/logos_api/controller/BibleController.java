package io.github.logos_api.controller;

import io.github.logos_api.dto.ChapterVerseResponseDTO;
import io.github.logos_api.service.LogosService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 책/장/절 단위 구조적 탐색을 위한 REST 컨트롤러입니다.
 * <p>
 * 없는 책이나 장인지는 {@link LogosService}가 판정해 예외로 알리고,
 * 상태 코드와 응답 형태는 전역 예외 처리기가 정합니다.
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
    public List<Integer> chapters(@PathVariable String book){
        return logosService.requireChapters(book);
    }

    /**
     * 지정한 책의 특정 장에 속한 전체 절을 반환합니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return 절 목록 (절 번호 + 본문). 등록되지 않은 책이거나 없는 장이면 404
     */
    @Operation(summary = "장 본문", description = "지정한 장의 절 번호와 본문을 돌려줍니다.")
    @GetMapping("/{book}/chapters/{chapter}")
    public List<ChapterVerseResponseDTO> verses(@PathVariable String book,
                                                @PathVariable int chapter){
        return logosService.requireChapterVerses(book, chapter);
    }
}
