package io.github.logos_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Logos {

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
}