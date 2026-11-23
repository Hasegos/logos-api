package io.github.logos_api.common.exception;

/**
 * 하루 키 발급 한도를 넘었을 때 던지는 예외입니다.
 * <p>
 * 한도를 넘은 것과 서버 내부 오류를 같은 예외로 던지면 컨트롤러가 둘을 구분하지 못하고
 * 내부 오류 메시지까지 한도 초과 응답에 실려 나가므로, 한도 초과만 이 예외로 따로 알립니다.
 * 상태 코드와 응답 형태는 {@link GlobalExceptionHandler}가 정합니다.
 * </p>
 */
public class KeyIssueLimitExceededException extends RuntimeException {

    /**
     * @param message 호출한 쪽에 그대로 보여 줄 안내 문구
     */
    public KeyIssueLimitExceededException(String message) {
        super(message);
    }
}
