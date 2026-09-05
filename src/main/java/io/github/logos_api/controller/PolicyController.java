package io.github.logos_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 이용약관, 개인정보처리방침 등 정책 관련 정적 페이지 요청을 처리하는 컨트롤러입니다.
 */
@Controller
public class PolicyController {

    /**
     * 이용약관 페이지를 반환합니다.
     *
     * @return 이용약관 템플릿 뷰 이름
     */
    @GetMapping("/terms")
    public String terms(){
        return "terms/terms";
    }

    /**
     * 개인정보처리방침 페이지를 반환합니다.
     *
     * @return 개인정보처리방침 템플릿 뷰 이름
     */
    @GetMapping("/privacy")
    public String privacy(){
        return "privacy/privacy";
    }
}