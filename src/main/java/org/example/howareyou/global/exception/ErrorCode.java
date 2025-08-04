package org.example.howareyou.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    /*──────────────────────── ErrorCode 작성 요령 ────────────────────────
     * ➡️  형식       :  ENUM_NAME(HttpStatus.XXX, "XNNN", "기본 메시지"),
     * ─────────────────────────────────────────────────────────────────────
     * 1) ENUM_NAME        : 대문자 스네이크 케이스  ― “도메인_원인” 형태로 짧고 명확히
     *                      예) USER_NOT_FOUND, AUTH_TOKEN_EXPIRED
     *
     * 2) HttpStatus.XXX   : 클라이언트에 내려줄 HTTP 상태
     *                      4xx → 사용자 잘못, 5xx → 서버/의존 서비스 오류
     *
     * 3) "XNNN"           : 서비스 내부 식별 코드 (문자 1 + 숫자 3)
     *                      · 첫 글자  : 도메인 구분자  (C=Common, U=User, A=Auth, P=Post …)
     *                      · 뒷 세 자리: 001, 002 …  순차 증가 (각 도메인별 독립 번호)
     *
     * 4) "기본 메시지"     : 사용자에게 보여 줄 기본 한글(또는 i18n 키) 메시지
     *                      · 너무 기술적인 내용은 ❌
     *                      · 예외 상세가 필요할 땐 CustomException detail 필드 사용
     *
     * 📌 새 에러 추가 절차
     *    ① 위 네 가지 규칙에 맞춰 ENUM 상수 한 줄 추가
     *    ② 필요하다면 전용 도메인 Exception 클래스에서 throw
     *    ③ 전역 핸들러(GlobalExceptionHandler)는 자동 처리
     *────────────────────────────────────────────────────────────────────*/

    /* ───────────[공통]─────────── */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,   "C001", "입력값이 유효하지 않습니다."),
    METHOD_NOT_ALLOWED   (HttpStatus.METHOD_NOT_ALLOWED,"C002", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR  (HttpStatus.INTERNAL_SERVER_ERROR,"C500","서버 오류가 발생했습니다."),


    /* ───────────[인증]─────────── */
    AUTH_BAD_CREDENTIAL        (HttpStatus.UNAUTHORIZED, "A001", "ID 또는 비밀번호가 일치하지 않습니다."),
    AUTH_TOKEN_EXPIRED         (HttpStatus.UNAUTHORIZED, "A002", "토큰이 만료되었습니다."),
    AUTH_INVALID_REFRESH_TOKEN (HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 리프레시 토큰입니다."),
    AUTH_EXPIRED_REFRESH_TOKEN (HttpStatus.UNAUTHORIZED, "A004", "만료된 리프레시 토큰입니다. 다시 로그인해주세요."),
    AUTH_SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND,  "A005", "연결된 소셜 계정을 찾을 수 없습니다."),
    DUPLICATE_EMAIL            (HttpStatus.CONFLICT,    "A006", "이미 가입된 이메일입니다."),
    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A007", "리프레시 토큰이 존재하지 않습니다."),

    /* ───────────[멤버]─────────── */
    MEMBER_NOT_FOUND       (HttpStatus.NOT_FOUND,   "M001", "해당 사용자를 찾을 수 없습니다."),
    DUPLICATED_MEMBERNAME  (HttpStatus.CONFLICT,    "M002", "이미 사용 중인 닉네임입니다."),
    
    /* ───────────[프로필]─────────── */
    PROFILE_NOT_FOUND     (HttpStatus.NOT_FOUND,     "P001", "프로필을 찾을 수 없습니다."),
    PROFILE_NOT_COMPLETED (HttpStatus.BAD_REQUEST,   "P002", "프로필 작성을 완료해주세요."),
    INVALID_NICKNAME      (HttpStatus.BAD_REQUEST,   "P003", "유효하지 않은 닉네임입니다."),
    /* ───────────[소셜 로그인]──────── */
    SOCIAL_AUTH_FAILED    (HttpStatus.UNAUTHORIZED,  "S001", "소셜 인증에 실패했습니다."),
    SOCIAL_EMAIL_REQUIRED (HttpStatus.BAD_REQUEST,   "S002", "이메일 제공에 동의해주세요."),
    UNSUPPORTED_PROVIDER  (HttpStatus.BAD_REQUEST,   "S003", "지원하지 않는 OAuth2 공급자입니다.");


    /* 필드 정의 */
    private final HttpStatus status;  // HTTP 응답용 상태
    private final String     code;    // 서비스 고유 코드(문자/숫자 혼용 가능)
    private final String     message; // 기본 메시지(한국어 or 영어)

    /* 생성자 */
    ErrorCode(HttpStatus status, String code, String message) {
        this.status  = status;
        this.code    = code;
        this.message = message;
    }

    /* Getter ― 럼북을 안 쓴 예시 */
    public HttpStatus getStatus() { return status; }
    public String     getCode()   { return code;   }
    public String     getMessage(){ return message;}
}