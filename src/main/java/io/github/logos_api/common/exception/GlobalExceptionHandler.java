package io.github.logos_api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 애플리케이션 전역 예외를 처리하는 핸들러.
 * <p>
 * 예외 종류에 따라 로그 레벨을 구분하여 기록하고,
 * /api/** 경로는 JSON 에러 응답, 그 외 경로는 에러 페이지를 렌더링한다.
 * </p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String API_PREFIX = "/api/";

    /**
     * 요청 URI가 API 경로인지 판별한다.
     *
     * @param request HTTP 요청 객체
     * @return API 경로 여부
     */
    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX);
    }

    /**
     * 존재하지 않는 경로 접근 시 발생하는 예외를 처리한다. (404)
     * <p>
     * 클라이언트 요청 오류이므로 {@code warn} 레벨로 기록한다.
     * API 경로: JSON 에러 응답 반환
     * 그 외 경로: 에러 페이지 렌더링
     * </p>
     *
     * @param request HTTP 요청 객체
     * @param e       발생한 예외
     * @param model   에러 정보를 뷰에 전달하기 위한 모델
     * @return API 요청 시 JSON 응답, 그 외 에러 뷰 이름
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleNotFound(HttpServletRequest  request, Exception e, Model model) {
        log.warn("[404] URI: {}", request.getRequestURI());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "path", request.getRequestURI()));
        }
        model.addAttribute("status", 404);
        model.addAttribute("title", "페이지를 찾을 수 없습니다");
        model.addAttribute("message", "요청하신 주소가 없거나 이동되었습니다.");
        return "error/error";
    }

    /**
     * 처리되지 않은 모든 예외를 처리한다. (500)
     * <p>
     * 예상치 못한 서버 오류이므로 {@code error} 레벨로 스택 트레이스와 함께 기록한다.
     * API 경로: JSON 에러 응답 반환
     * 그 외 경로: 에러 페이지 렌더링
     * </p>
     *
     * @param request HTTP 요청 객체
     * @param ex      발생한 예외
     * @param model   에러 정보를 뷰에 전달하기 위한 모델
     * @return API 요청 시 JSON 응답, 그 외 에러 뷰 이름
     */
    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(HttpServletRequest request, Exception ex, Model model) {
        log.error("[500] Unexpected error. URI: {}", request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error"));
        }
        model.addAttribute("status", 500);
        model.addAttribute("title", "서버 오류가 발생했습니다");
        model.addAttribute("message", "잠시 후 다시 시도해 주세요.");
        return "error/error";
    }
}