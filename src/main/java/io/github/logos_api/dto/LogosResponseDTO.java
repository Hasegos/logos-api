package io.github.logos_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.logos_api.model.Logos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @JsonProperty(value = "reference", access = JsonProperty.Access.READ_ONLY)
    public String getReference(){
        if(book == null || chapter == null || verse == null){
            return null;
        }
        return String.format("%s %s:%s", this.book, this.chapter, this.verse);
    }

   public static LogosResponseDTO from(Logos logos){
        return LogosResponseDTO.builder()
                .book(logos.getBook())
                .chapter(logos.getChapter())
                .text(logos.getText())
                .verse(logos.getVerse())
                .build();
   }
}