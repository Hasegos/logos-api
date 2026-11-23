package io.github.logos_api.controller;

import io.github.logos_api.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

/**
 * API 문서 페이지 요청을 처리하는 컨트롤러.
 * 언어별 예제는 찾는 말이 서로 달라("성경 api 자바", "성경 api 파이썬") 각각 다른 주소로 둔다.
 */
@Controller
public class DocsController {

    /**
     * 예제를 제공하는 언어 목록입니다. 주소로 템플릿을 고르므로 허용 목록으로 제한합니다.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "javascript", "python");

    /**
     * API 사용법 문서 페이지를 반환한다.
     *
     * @return docs 템플릿 뷰 이름
     */
    @GetMapping("/docs")
    public String docs(){
        return "docs/docs";
    }

    /**
     * 언어별 연동 예제 페이지를 반환한다.
     *
     * @param language 언어 이름 (java, javascript, python)
     * @return 해당 언어의 템플릿 뷰 이름
     * @throws ResourceNotFoundException 지원하지 않는 언어인 경우
     */
    @GetMapping("/docs/{language}")
    public String languageGuide(@PathVariable String language){
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new ResourceNotFoundException("지원하지 않는 언어입니다.");
        }
        return "docs/" + language;
    }
}
