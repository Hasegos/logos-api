package io.github.logos_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 구절 검색 API의 응답에 사용되는 DTO입니다.
 * 전체 일치 건수와 페이지 정보를 함께 담아, 클라이언트가 다음 페이지를 계산할 수 있게 합니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResponseDTO {

    @JsonProperty("query")
    private String query;

    @JsonProperty("total")
    private int total;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("results")
    private List<LogosResponseDTO> results;

    /**
     * 책 이름 -> 그 책에서 검색어가 나온 절 수. book 필터와 무관하게 전체 기준으로 센다.
     */
    @JsonProperty("books")
    private Map<String, Integer> books;
}
