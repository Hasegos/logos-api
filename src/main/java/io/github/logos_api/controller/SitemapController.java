package io.github.logos_api.controller;

import io.github.logos_api.service.SitemapService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 사이트맵 요청을 처리하는 컨트롤러.
 * 어떤 주소를 담을지는 {@link SitemapService}가 정하고, 여기서는 응답으로만 내보낸다.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    /**
     * sitemap.xml을 반환한다.
     *
     * @return XML 형식의 사이트맵 문자열
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        return sitemapService.build();
    }
}
