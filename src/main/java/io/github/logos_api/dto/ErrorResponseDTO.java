package io.github.logos_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오류 응답의 형태를 정하는 DTO입니다.
 * <p>
 * 인터셉터와 전역 예외 처리기가 각자 본문을 만들면 같은 API가 오류마다 다른 필드 이름을 내보내게 되므로,
 * 모든 오류 응답은 이 형태 하나로 맞춥니다.
 * {@code error}는 HTTP 상태 이름으로 고정해 호출하는 쪽이 분기에 쓰고,
 * {@code message}는 사람이 읽는 안내라 문구가 바뀔 수 있습니다.
 * </p>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    /**
     * 오류 응답 본문을 만듭니다.
     *
     * @param error   HTTP 상태 이름 (예: Not Found). 호출하는 쪽의 분기 기준이라 바꾸지 않습니다
     * @param message 호출한 쪽이 다음에 무엇을 하면 되는지 알려 주는 한국어 안내
     * @return 오류 응답 본문
     */
    public static ErrorResponseDTO of(String error, String message){
        return new ErrorResponseDTO(error, message);
    }
}
