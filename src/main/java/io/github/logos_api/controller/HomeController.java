package io.github.logos_api.controller;

import io.github.logos_api.service.LogosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 웹 애플리케이션의 루트 경로 요청을 처리하고 홈 화면을 제공하는 컨트롤러입니다.
 * 총 구절 수와 오늘의 말씀을 서버사이드에서 미리 계산해 뷰에 전달합니다.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final LogosService logosService;

    /**
     * 루트 경로("/")로 들어오는 GET 요청을 처리하여 홈 화면으로 이동시킵니다.
     *
     * @param model 총 구절 수(totalVerses), 오늘의 말씀(todayVerse)을 담아 뷰에 전달
     * @return 홈 화면을 나타내는 뷰(View)의 이름 ("home")
     */
    @GetMapping("/")
    public String showHome(Model model){
        model.addAttribute("totalVerses", logosService.getTotalVerseCount());
        model.addAttribute("todayVerse", logosService.getTodayVerse());
        return "home";
    }
}