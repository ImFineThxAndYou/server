package org.example.howareyou.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ── 1) 우리 서비스가 던진 CustomException ───────────────────────── */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {

        ErrorCode ec = ex.getErrorCode();
        ErrorResponse body = ErrorResponse.builder()
                .code(ec.getCode())
                .message(ec.getMessage())
                .status(ec.getStatus().value())
                .detail(ex.getDetail())
                .timestamp(LocalDateTime.now())
                .build();

        /* 서버 로그로는 detail·stackTrace를 남겨 두자 */
        log.warn("[{}] {}", ec.getCode(), ex.getDetail(), ex);

        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    /* ── 2) @Valid 바인딩 오류 ───────────────────────────────────────── */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> String.format("%s(%s)", err.getDefaultMessage(), err.getField()))
                .collect(Collectors.joining(", "));

        return makeValidationErrorResponse(detail);
    }

    /* ── 3) @Validated(JSR-380) 위반 ──────────────────────────────────── */
    @ExceptionHandler(ConstraintViolationException.class) // ✅ import 수정
    protected ResponseEntity<ErrorResponse> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {

        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())           // 각 제약 메시지
                .collect(Collectors.joining(", ")); // "필드는 필수입니다, 길이가..." 형태

        return makeValidationErrorResponse(detail); // 기존 헬퍼 재사용
    }

    /* ── 4) SSE 관련 예외 처리 ────────────────────────────────────────── */
    @ExceptionHandler({AsyncRequestNotUsableException.class, IOException.class})
    protected ResponseEntity<ErrorResponse> handleSseException(Exception ex) {

        // SSE 연결이 끊어진 경우 (클라이언트가 브라우저를 닫거나 네트워크 문제)
        if (ex instanceof AsyncRequestNotUsableException ||
            (ex instanceof IOException && ex.getMessage() != null &&
             (ex.getMessage().contains("Broken pipe") || ex.getMessage().contains("Connection reset")))) {

            log.debug("[SSE] Client disconnected: {}", ex.getMessage());

            // SSE 연결 끊김은 정상적인 상황이므로 에러로 로깅하지 않음
            // 빈 응답을 반환하여 추가 에러 로그 방지
            return ResponseEntity.noContent().build();
        }

        // 기타 SSE 관련 예외
        log.warn("[SSE] SSE error: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .code("SSE_ERROR")
                .message("실시간 연결에 문제가 발생했습니다.")
                .status(500)
                .detail("SSE connection error")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(500).body(body);
    }

    /* ── 5) Redis/Session 관련 예외 ───────────────────────────────────── */
    @ExceptionHandler({RedisSystemException.class, IllegalStateException.class})
    protected ResponseEntity<ErrorResponse> handleRedisSessionException(Exception ex) {
        
        // Session was invalidated 에러는 클라이언트에게 세션 만료로 안내
        if (ex.getMessage() != null && ex.getMessage().contains("Session was invalidated")) {
            log.warn("[SESSION] Session invalidated: {}", ex.getMessage());
            
            ErrorResponse body = ErrorResponse.builder()
                    .code("SESSION_EXPIRED")
                    .message("세션이 만료되었습니다. 다시 로그인해주세요.")
                    .status(401)
                    .detail("Session was invalidated")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.status(401).body(body);
        }
        
        // Redis 연결 문제
        log.error("[REDIS] Redis/Session error: {}", ex.getMessage(), ex);
        
        ErrorResponse body = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("서버 연결에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .detail("Redis connection issue")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(body);
    }

    /* ── 5) 예상 못한 모든 예외(Fallback) ─────────────────────────────── */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception ex) {

        log.error("[UNEXPECTED] {}", ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .detail(ex.getClass().getSimpleName()) // 구현 시 필요에 맞게 변경
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(body);
    }

    /* 공통: Validation 오류 응답을 만들어 주는 헬퍼 */
    private ResponseEntity<ErrorResponse> makeValidationErrorResponse(String detail) {

        ErrorResponse body = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(ErrorCode.INVALID_INPUT_VALUE.getMessage())
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus().value())
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(body);
    }
}