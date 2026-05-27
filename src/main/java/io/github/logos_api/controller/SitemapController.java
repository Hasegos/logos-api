package io.github.logos_api.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 사이트맵 요청을 처리하는 컨트롤러.
 * logos-api는 단일 페이지 서비스이므로 정적으로 생성한다.
 */
@Controller
public class SitemapController {

    private static final String BASE_URL = "https://logos-api.com";
    private static final String CHANGE_FREQ_WEEKLY = "weekly";

    /**
     * sitemap.xml을 생성하여 반환한다.
     *
     * @return XML 형식의 사이트맵 문자열
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        sb.append(url(BASE_URL + "/", CHANGE_FREQ_WEEKLY, "1.0"));
        sb.append("</urlset>");
        return sb.toString();
    }

    /**
     * 단일 {@code <url>} XML 블록을 생성한다.
     *
     * @param loc        페이지 URL
     * @param changefreq 변경 빈도
     * @param priority   우선순위 (0.0 ~ 1.0)
     * @return XML 형식의 url 블록 문자열
     */
    private String url(String loc, String changefreq, String priority) {
        return "<url>"
                + "<loc>" + escapeXml(loc) + "</loc>"
                + "<changefreq>" + changefreq + "</changefreq>"
                + "<priority>" + priority + "</priority>"
                + "</url>";
    }

    /**
     * XML 특수문자를 이스케이프 처리한다.
     *
     * @param value 이스케이프할 문자열
     * @return 이스케이프 처리된 문자열
     */
    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}