package io.github.logos_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 웹 애플리케이션의 루트 경로 요청을 처리하고 홈 화면을 제공하는 컨트롤러입니다.
 * 주로 정적인 HTML 페이지(Thymeleaf 등)를 렌더링하기 위한 기본 진입점 역할을 수행합니다.
 */
@Controller
public class HomeController {

    /**
     * 루트 경로("/")로 들어오는 GET 요청을 처리하여 홈 화면으로 이동시킵니다.
     *
     * @return 홈 화면을 나타내는 뷰(View)의 이름 ("home")
     */
    @GetMapping("/")
    public String showHome(){
        return "home";
    }
}