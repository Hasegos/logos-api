package io.github.logos_api.common.exception;

/**
 * 요청한 자원이 없을 때 던지는 예외입니다.
 * <p>
 * 있는지 없는지는 데이터를 가진 쪽만 알 수 있으므로 판정은 서비스에서 하고,
 * 상태 코드와 응답 형태는 {@link GlobalExceptionHandler}가 한곳에서 정합니다.
 * 컨트롤러마다 없을 때의 분기를 두지 않기 위한 통로입니다.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message 호출한 쪽에 그대로 보여 줄 안내 문구
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
