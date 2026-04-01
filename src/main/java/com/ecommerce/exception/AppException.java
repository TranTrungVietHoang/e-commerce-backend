package com.ecommerce.exception;

import lombok.Getter;

/**
 * Exception chung cho ứng dụng.
 * Mọi lỗi nghiệp vụ đều throw AppException(ErrorCode.XXX) để 
 * GlobalExceptionHandler có thể bắt và trả về response chuẩn.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}