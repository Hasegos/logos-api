package io.github.logos_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 검색 엔진에 넘길 사이트맵을 만드는 서비스입니다.
 * <p>
 * 어떤 주소를 담을지, 어느 것을 더 자주 보게 할지는 콘텐츠를 아는 쪽이 정해야 하므로
 * 조립을 서비스에서 하고 컨트롤러는 결과를 응답으로만 내보냅니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SitemapService {

    private static final String BASE_URL = "https://logos-api.com";
    private static final String CHANGE_FREQ_WEEKLY = "weekly";
    private static final String CHANGE_FREQ_YEARLY = "yearly";

    /**
     * 본문과 화면은 배포할 때만 바뀌므로 애플리케이션이 뜬 날짜를 마지막 수정일로 씁니다.
     * 크롤러가 변경 여부를 판단할 수 있어야 1,189개 장 페이지를 다시 훑는 주기가 짧아집니다.
     */
    private static final String LAST_MODIFIED = LocalDate.now().toString();

    private final LogosService logosService;

    /**
     * 고정 페이지와 등록된 모든 책 · 장의 읽기 페이지를 담은 사이트맵 XML을 만듭니다.
     * 개인 기록 화면(`/read/plan`)과 검색 결과(`/read/search`)는 색인 대상이 아니므로 넣지 않습니다.
     *
     * @return 사이트맵 XML 문자열
     */
    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        sb.append(url(BASE_URL + "/", CHANGE_FREQ_WEEKLY, "1.0"));
        sb.append(url(BASE_URL + "/docs", CHANGE_FREQ_WEEKLY, "0.8"));
        sb.append(url(BASE_URL + "/read", CHANGE_FREQ_WEEKLY, "0.7"));
        sb.append(url(BASE_URL + "/terms", CHANGE_FREQ_YEARLY, "0.3"));
        sb.append(url(BASE_URL + "/privacy", CHANGE_FREQ_YEARLY, "0.3"));

        for (String book : logosService.getAvailableBooks()) {
            String encodedBook = UriUtils.encodePathSegment(book, StandardCharsets.UTF_8);
            for (int chapter : logosService.getChapters(book)) {
                sb.append(url(BASE_URL + "/read/" + encodedBook + "/" + chapter, CHANGE_FREQ_YEARLY, "0.6"));
            }
        }

        sb.append("</urlset>");
        return sb.toString();
    }

    /**
     * 단일 {@code <url>} 블록을 만듭니다.
     *
     * @param loc        페이지 주소
     * @param changefreq 변경 빈도
     * @param priority   우선순위 (0.0 ~ 1.0)
     * @return url 블록 XML
     */
    private String url(String loc, String changefreq, String priority) {
        return "<url>"
                + "<loc>" + escapeXml(loc) + "</loc>"
                + "<lastmod>" + LAST_MODIFIED + "</lastmod>"
                + "<changefreq>" + changefreq + "</changefreq>"
                + "<priority>" + priority + "</priority>"
                + "</url>";
    }

    /**
     * XML에서 뜻이 달라지는 문자를 바꿉니다.
     *
     * @param value 바꿀 문자열
     * @return 치환된 문자열
     */
    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
