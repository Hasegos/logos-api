package io.github.logos_api.controller;

import io.github.logos_api.model.Logos;
import io.github.logos_api.service.LogosService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapControllerTest {

    @Test
    void sitemapListsEveryChapterPage() throws Exception {
        LogosService service = new LogosService();
        service.init();
        int chapters = service.getAvailableBooks().stream().mapToInt(book -> service.getChapters(book).size()).sum();

        String xml = new SitemapController(service).sitemap();

        assertEquals(5 + chapters, xml.split("<url>", -1).length - 1);
        assertTrue(xml.contains("<loc>https://logos-api.com/read/%EC%B0%BD%EC%84%B8%EA%B8%B0/1</loc>"));
    }

    @Test
    void chapterDescriptionCutsLongFirstVerse() {
        List<Logos> verses = List.of(new Logos("창세기", "1", "1", "가".repeat(100)));

        assertEquals("창세기 1장 본문. " + "가".repeat(70) + "…", ReadController.chapterDescription("창세기", 1, verses));
    }
}