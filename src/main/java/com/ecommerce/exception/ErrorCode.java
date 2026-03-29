package com.ecommerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Tập trung toàn bộ mã lỗi nghiệp vụ của hệ thống.
 * Mỗi enum là một loại lỗi có: HTTP status + message mặc định.
 * 
 * CÁCH DÙNG: throw new AppException(ErrorCode.EMAIL_EXISTS);
 */
@Getter
public enum ErrorCode {

    // =========================================================
    // COMMON (1xxx)
    // =========================================================
    UNCATEGORIZED_ERROR(1000, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Tài nguyên không tồn tại", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(1003, "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // =========================================================
    // AUTH (2xxx)
    // =========================================================
    EMAIL_EXISTS(2001, "Email này đã được đăng ký", HttpStatus.CONFLICT),
    PHONE_EXISTS(2002, "Số điện thoại này đã được sử dụng", HttpStatus.CONFLICT),
    BAD_CREDENTIALS(2003, "Sai email hoặc mật khẩu", HttpStatus.UNAUTHORIZED),
    USER_LOCKED(2004, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(2005, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2006, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),

    // =========================================================
    // USER (3xxx)
    // =========================================================
    USER_NOT_FOUND(3001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    WRONG_CURRENT_PASSWORD(3002, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(3003, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),

    // =========================================================
    // ADMIN (4xxx)
    // =========================================================
    CANNOT_LOCK_ADMIN(4001, "Không thể khóa tài khoản Admin khác", HttpStatus.FORBIDDEN),

    // =========================================================
    // OTP (5xxx)
    // =========================================================
    OTP_INVALID(5001, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(5002, "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới", HttpStatus.BAD_REQUEST);

    // =========================================================

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
