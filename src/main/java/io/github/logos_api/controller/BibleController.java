package io.github.logos_api.controller;

import io.github.logos_api.dto.ChapterVerseResponseDTO;
import io.github.logos_api.service.LogosService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 책/장/절 단위 구조적 탐색을 위한 REST 컨트롤러입니다.
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
    @GetMapping
    public List<String> books() {
        return logosService.getAvailableBooks();
    }

    /**
     * 지정한 책에 존재하는 장 번호 목록을 반환합니다.
     *
     * @param book 책 이름
     * @return 장 번호 목록
     */
    @GetMapping("/{book}/{chapters}")
    public List<Integer> chapters(@PathVariable String book){
        return logosService.getChapters(book);
    }

    /**
     * 지정한 책의 특정 장에 속한 전체 절을 반환합니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return 절 목록 (절 번호 + 본문)
     */
    @GetMapping("/{book}/chapters/{chapter}")
    public List<ChapterVerseResponseDTO> verse(@PathVariable String book,
                                               @PathVariable int chapter){
        return logosService.getVerses(book, chapter).stream()
                .map(ChapterVerseResponseDTO::from)
                .collect(Collectors.toList());
    }
}