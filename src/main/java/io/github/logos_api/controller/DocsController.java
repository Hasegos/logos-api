package io.github.logos_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * API 문서 페이지 요청을 처리하는 컨트롤러.
 */
@Controller
public class DocsController {

    /**
     * API 사용법 문서 페이지를 반환한다.
     *
     * @return docs 템플릿 뷰 이름
     */
    @GetMapping("/docs")
    public String docs(){
        return "docs/docs";
    }
}