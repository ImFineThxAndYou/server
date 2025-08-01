package org.example.howareyou.global.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final String        code;      // "U001", "C003" …
    private final String        message;   // 사용자 메시지
    private final int           status;    // 400, 404 …
    private final String        detail;    // 개발자용 상세(선택)
    private final LocalDateTime timestamp; // 오류 발생 시각
}