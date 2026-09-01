package io.github.logos_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import io.github.logos_api.model.Logos;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 책·장·절 조회 API의 절 목록 응답에 사용되는 경량 DTO입니다.
 * book/chapter는 요청 URL에 이미 포함되어 있으므로 verse/text만 담습니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChapterVerseResponseDTO {

    @JsonProperty("verse")
    private String verse;

    @JsonProperty("text")
    private String text;

    /**
     * Logos 도메인 엔티티를 응답 DTO로 변환합니다.
     *
     * @param logos 성경 구절 엔티티
     * @return 변환된 DTO 객체
     */
    public static ChapterVerseResponseDTO from(Logos logos){
        return ChapterVerseResponseDTO.builder()
                .verse(logos.getVerse())
                .text(logos.getText())
                .build();
    }
}