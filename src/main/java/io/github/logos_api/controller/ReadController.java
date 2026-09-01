package io.github.logos_api.controller;

import io.github.logos_api.service.LogosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 책/장/절을 직접 탐색하며 읽는 "성경 읽기" 페이지를 처리하는 컨트롤러입니다.
 */
@Controller
@RequestMapping("/read")
@RequiredArgsConstructor
public class ReadController {

    private final LogosService logosService;

    /**
     * 구약/신약으로 그룹핑된 책 목록과, 각 책에 존재하는 장 번호 목록을 함께 내려줍니다.
     * 장 번호는 아코디언에서 데이터 유무에 따라 클릭 가능 여부를 구분하는 데 사용됩니다.
     *
     * @param model 뷰에 전달할 모델
     * @return 책 목록 템플릿 뷰 이름
     */
    @GetMapping
    public String list(Model model){
        Map<String, List<String>> bookByTestament = logosService.getAvailableBooksByTestament();
        Map<String, List<Integer>> chaptersByBook = new LinkedHashMap<>();
        bookByTestament.values().forEach(books ->
                books.forEach(book -> chaptersByBook.put(book, logosService.getChapters(book)))
        );
        model.addAttribute("booksByTestament", bookByTestament);
        model.addAttribute("chaptersByBook", chaptersByBook);

        return "read/read";
    }

    /**
     * 특정 책/장의 전체 절과, 이전/다음 장 이동 정보를 함께 내려줍니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @param model   뷰에 전달할 모델
     * @return 장 상세 템플릿 뷰 이름
     */
    @GetMapping("/{book}/{chapter}")
    public String chapter(@PathVariable String book,
                          @PathVariable int chapter,
                          Model model){
        List<Integer> chapters = logosService.getChapters(book);
        int idx = chapters.indexOf(chapter);
        model.addAttribute("book", book);
        model.addAttribute("chapter", chapter);
        model.addAttribute("verses", logosService.getVerses(book,chapter));
        model.addAttribute("prevChapter", idx > 0 ? chapters.get(idx - 1) : null);
        model.addAttribute("nextChapter", (idx >= 0 && idx < chapters.size() - 1) ? chapters.get(idx + 1) : null);

        return "read/chapter";
    }
}