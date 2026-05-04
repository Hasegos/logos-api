package io.github.logos_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.logos_api.model.Logos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 성경 구절 정보를 클라이언트에게 응답하기 위한 데이터 전송 객체(DTO)입니다.
 * {@link #getReference()} 메서드를 통해 '창세기 1:1'과 같은 형식의 참조 문자열을 제공합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogosResponseDTO {

    @JsonProperty("book")
    private String book;

    @JsonProperty("chapter")
    private String chapter;

    @JsonProperty("verse")
    private String verse;

    @JsonProperty("text")
    private String text;

    /**
     * '권 장:절' 형식의 참조 문자열을 반환합니다. (예: 요한복음 3:16)
     *
     * @return 성경 구절 참조 문자열
     */
    @JsonProperty(value = "reference", access = JsonProperty.Access.READ_ONLY)
    public String getReference(){
        if(book == null || chapter == null || verse == null){
            return null;
        }
        return String.format("%s %s:%s", this.book, this.chapter, this.verse);
    }

    /**
     * Logos 도메인 엔티티를 응답 DTO로 변환합니다.
     *
     * @param logos 성경 구절 엔티티
     * @return 변환된 DTO 객체
     */
    public static LogosResponseDTO from(Logos logos){
        return LogosResponseDTO.builder()
                .book(logos.getBook())
                .chapter(logos.getChapter())
                .text(logos.getText())
                .verse(logos.getVerse())
                .build();
    }
}