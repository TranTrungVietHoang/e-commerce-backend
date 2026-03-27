package com.ecommerce.exception;

import lombok.Getter;

/**
 * Custom RuntimeException cho toàn bộ dự án.
 * 
 * Thay vì throw generic Exception, mọi lỗi nghiệp vụ đều
 * throw AppException(ErrorCode.XXX) để GlobalExceptionHandler
 * có thể bắt và trả về response chuẩn cho FE.
 * 
 * CÁCH DÙNG:
 *   throw new AppException(ErrorCode.EMAIL_EXISTS);
 *   throw new AppException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        // Truyền message của ErrorCode vào parent RuntimeException
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
