package io.github.logos_api.common.exception;

import io.github.logos_api.common.config.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
    private static final int MAX_UA_LENGTH = 200;

    /**
     * 로그 위조(개행 삽입)를 막기 위해 제어문자를 치환한다.
     *
     * @param value 로그에 남길 값
     * @return 개행과 탭이 밑줄로 바뀐 값
     */
    private static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }

    /**
     * User-Agent를 로그용으로 정제한다. 누가 보낸 요청인지 IP와 함께 보기 위해 남긴다.
     *
     * @param request HTTP 요청 객체
     * @return 정제된 User-Agent (없으면 "unknown")
     */
    private static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String cleaned = sanitize(ua);
        return cleaned.substring(0, Math.min(cleaned.length(), MAX_UA_LENGTH));
    }

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
     * 봇의 취약점 스캔으로도 흔히 발생하므로 {@code info} 레벨로 기록한다.
     * {@code warn}으로 남기면 error.log가 스캐너 요청으로 덮여 실제 장애가 묻힌다.
     * 어떤 IP가 무엇을 긁었는지는 이 줄과 page-access.log의 {@code status=404}로 확인한다.
     * API 경로: JSON 에러 응답 반환
     * 그 외 경로: 에러 페이지 렌더링
     * </p>
     *
     * @param request HTTP 요청 객체
     * @param e       발생한 예외
     * @param model   에러 정보를 뷰에 전달하기 위한 모델
     * @return API 요청 시 JSON 응답, 그 외 에러 뷰 이름
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleNotFound(HttpServletRequest  request, Exception e, Model model) {
        log.info("[404] ip={} uri={} ua={}", ClientIpResolver.resolve(request), sanitize(request.getRequestURI()), userAgent(request));
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
     * 경로 변수/파라미터의 타입이 맞지 않는 경우를 처리한다. (400)
     * <p>
     * 예: {@code /read/창세기/abc}처럼 장 번호 자리에 숫자가 아닌 값이 들어온 경우.
     * 클라이언트 요청 형식 오류이므로 {@code warn} 레벨로 기록하고, 서버 오류(500)가 아닌
     * 잘못된 요청(400)으로 정확히 응답한다.
     * API 경로: JSON 에러 응답 반환
     * 그 외 경로: 에러 페이지 렌더링
     * </p>
     *
     * @param request HTTP 요청 객체
     * @param e       발생한 예외
     * @param model   에러 정보를 뷰에 전달하기 위한 모델
     * @return API 요청 시 JSON 응답, 그 외 에러 뷰 이름
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(HttpServletRequest request, MethodArgumentTypeMismatchException e, Model model) {
        log.warn("[400] 잘못된 요청 파라미터 - ip={} uri={} 파라미터명={}", ClientIpResolver.resolve(request), sanitize(request.getRequestURI()), e.getName());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "path", request.getRequestURI()));
        }
        model.addAttribute("status", 400);
        model.addAttribute("title", "잘못된 요청입니다");
        model.addAttribute("message", "요청하신 주소의 형식이 올바르지 않습니다.");
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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(HttpServletRequest request, Exception ex, Model model) {
        log.error("[500] Unexpected error. ip={} uri={}", ClientIpResolver.resolve(request), sanitize(request.getRequestURI()), ex);
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