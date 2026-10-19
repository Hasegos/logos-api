package io.github.logos_api.controller;

import io.github.logos_api.model.Logos;
import io.github.logos_api.service.LogosService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Duration;
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

    private static final String PAGE_CACHE_CONTROL = CacheControl.maxAge(Duration.ofHours(1)).cachePublic().getHeaderValue();
    private static final int DESCRIPTION_SNIPPET_LENGTH = 70;

    private final LogosService logosService;

    /**
     * 구약/신약으로 그룹핑된 책 목록과, 각 책에 존재하는 장 번호 목록을 함께 내려줍니다.
     * 장 번호는 아코디언에서 데이터 유무에 따라 클릭 가능 여부를 구분하는 데 사용됩니다.
     * 본문은 배포 때만 바뀌므로 응답에 1시간 캐시 헤더를 붙인다.
     *
     * @param model    뷰에 전달할 모델
     * @param response 캐시 헤더를 설정할 응답
     * @return 책 목록 템플릿 뷰 이름
     */
    @GetMapping
    public String list(Model model, HttpServletResponse response){
        Map<String, List<String>> bookByTestament = logosService.getAvailableBooksByTestament();
        Map<String, List<Integer>> chaptersByBook = new LinkedHashMap<>();
        bookByTestament.values().forEach(books ->
                books.forEach(book -> chaptersByBook.put(book, logosService.getChapters(book)))
        );
        model.addAttribute("booksByTestament", bookByTestament);
        model.addAttribute("chaptersByBook", chaptersByBook);
        response.setHeader(HttpHeaders.CACHE_CONTROL, PAGE_CACHE_CONTROL);

        return "read/read";
    }

    /**
     * 특정 책/장의 전체 절과, 이전/다음 장 이동 정보를 함께 내려줍니다.
     * 존재하는 장일 때만 응답에 1시간 캐시 헤더를 붙이고, 없는 책이나 장은 404로 응답한다.
     * meta description에는 첫 절의 앞부분을 넣어 장마다 다른 문장이 되게 한다.
     *
     * @param book     책 이름
     * @param chapter  장 번호
     * @param model    뷰에 전달할 모델
     * @param response 캐시 헤더를 설정할 응답
     * @return 장 상세 템플릿 뷰 이름
     */
    @GetMapping("/{book}/{chapter}")
    public String chapter(@PathVariable String book,
                          @PathVariable int chapter,
                          Model model,
                          HttpServletResponse response) throws NoResourceFoundException {
        List<Logos> verses = logosService.getVerses(book, chapter);
        if (verses.isEmpty()) {
            throw new NoResourceFoundException(HttpMethod.GET, "/read/" + book + "/" + chapter);
        }

        response.setHeader(HttpHeaders.CACHE_CONTROL, PAGE_CACHE_CONTROL);
        List<Integer> chapters = logosService.getChapters(book);
        int idx = chapters.indexOf(chapter);
        model.addAttribute("book", book);
        model.addAttribute("chapter", chapter);
        model.addAttribute("verses", verses);
        model.addAttribute("description", chapterDescription(book, chapter, verses));
        model.addAttribute("prevChapter", idx > 0 ? chapters.get(idx - 1) : null);
        model.addAttribute("nextChapter", (idx >= 0 && idx < chapters.size() - 1) ? chapters.get(idx + 1) : null);

        return "read/chapter";
    }

    /**
     * 장 페이지의 meta description을 만든다. 첫 절의 앞 {@value #DESCRIPTION_SNIPPET_LENGTH}자를 붙이고, 더 길면 말줄임표로 끝낸다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @param verses  절 번호 순으로 정렬된 비어 있지 않은 절 목록
     * @return 장마다 다른 description 문장
     */
    static String chapterDescription(String book, int chapter, List<Logos> verses) {
        String first = verses.get(0).getText() == null ? "" : verses.get(0).getText().strip();
        String snippet = first.length() > DESCRIPTION_SNIPPET_LENGTH
                ? first.substring(0, DESCRIPTION_SNIPPET_LENGTH) + "…"
                : first;
        return (book + " " + chapter + "장 본문. " + snippet).strip();
    }
}