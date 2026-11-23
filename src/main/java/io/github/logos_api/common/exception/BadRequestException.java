package io.github.logos_api.common.exception;

/**
 * 요청 값이 잘못되었을 때 던지는 예외입니다.
 * <p>
 * 값이 올바른지는 규칙을 가진 서비스가 판정하고,
 * 상태 코드와 응답 형태는 {@link GlobalExceptionHandler}가 한곳에서 정합니다.
 * </p>
 */
public class BadRequestException extends RuntimeException {

    /**
     * @param message 호출한 쪽에 그대로 보여 줄 안내 문구
     */
    public BadRequestException(String message) {
        super(message);
    }
}
