package io.github.logos_api.controller;

import io.github.logos_api.service.OgImageService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.Duration;

/**
 * 장 페이지를 공유할 때 쓰는 이미지를 돌려주는 컨트롤러입니다.
 * 사람이 보는 화면이 아니라 공유 카드용 이미지라 OpenAPI 스펙에서는 제외합니다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class OgImageController {

    private final OgImageService ogImageService;

    /**
     * 지정한 장의 공유 이미지를 PNG로 돌려줍니다.
     * 본문은 배포할 때만 바뀌므로 30일 캐시를 붙입니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return PNG 이미지
     * @throws NoResourceFoundException 해당 장이 없는 경우
     * @throws IOException              이미지를 그리는 중 오류가 난 경우
     */
    @GetMapping(value = "/og/read/{book}/{chapter}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> chapterImage(@PathVariable String book,
                                               @PathVariable int chapter) throws NoResourceFoundException, IOException {
        byte[] image = ogImageService.render(book, chapter);
        if (image.length == 0) {
            throw new NoResourceFoundException(HttpMethod.GET, "/og/read/" + book + "/" + chapter + ".png");
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
