package com.example.air.exception;

/** 애플리케이션 예외 모음 (GlobalExceptionHandler 에서 화면으로 변환) */
public final class AppExceptions {

    private AppExceptions() {
    }

    /** 데이터 없음 → 404 */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    /** 업로드 파일 오류 → 400 */
    public static class InvalidFileException extends RuntimeException {
        public InvalidFileException(String message) { super(message); }
    }

    /** 중복 데이터 → 409 */
    public static class DuplicateException extends RuntimeException {
        public DuplicateException(String message) { super(message); }
    }

    /** AI 서버 통신 실패 → 503 */
    public static class AiServerException extends RuntimeException {
        public AiServerException(String message, Throwable cause) { super(message, cause); }
    }
}
