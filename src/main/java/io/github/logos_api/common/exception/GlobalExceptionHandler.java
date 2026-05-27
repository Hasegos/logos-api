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

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String API_PREFIX = "/api/";

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX);
    }

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