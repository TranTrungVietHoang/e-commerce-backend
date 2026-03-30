package com.ecommerce.exception;

/**
 * Mã lỗi được sử dụng trong ứng dụng
 */
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized error"),
    INVALID_KEY("1000", "Invalid message key"),
    INVALID_USERNAME("1001", "Username should be at least 3 characters"),
    INVALID_PASSWORD("1002", "Password should be at least 8 characters"),
    INVALID_EMAIL("1003", "Invalid email format"),
    USER_NOT_FOUND("1004", "User not found"),
    USER_ALREADY_EXISTS("1005", "User already exists"),
    INVALID_TOKEN("1006", "Invalid or expired token"),
    UNAUTHORIZED("1007", "Unauthorized"),
    FORBIDDEN("1008", "Forbidden"),
    PASSWORD_NOT_MATCH("1009", "Password does not match"),
    OTP_INVALID("1010", "OTP is invalid"),
    OTP_EXPIRED("1011", "OTP is expired"),
    OTP_INVALID_OR_EXPIRED("1012", "OTP is invalid or expired"),
    VERIFICATION_TOKEN_NOT_FOUND("1013", "Verification token not found"),
    ;

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
