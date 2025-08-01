package org.example.howareyou.global.exception;

public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;   // 어떤 에러인지 식별
    private final String    detail;      // 추가 설명(선택)

    /* 필수 정보만 받는 생성자 */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail    = null;
    }

    /* 상세 메시지를 함께 전달하는 생성자 */
    public CustomException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.detail    = detail;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String    getDetail()    { return detail;    }
}