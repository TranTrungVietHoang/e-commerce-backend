package com.ecommerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Tập trung toàn bộ mã lỗi nghiệp vụ của hệ thống.
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
    INVALID_KEY(1004, "Message key không hợp lệ", HttpStatus.BAD_REQUEST),

    // =========================================================
    // AUTH (2xxx)
    // =========================================================
    EMAIL_EXISTS(2001, "Email này đã được đăng ký", HttpStatus.CONFLICT),
    PHONE_EXISTS(2002, "Số điện thoại này đã được sử dụng", HttpStatus.CONFLICT),
    BAD_CREDENTIALS(2003, "Sai email hoặc mật khẩu", HttpStatus.UNAUTHORIZED),
    USER_LOCKED(2004, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(2005, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2006, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_EXISTS(2007, "Người dùng đã tồn tại", HttpStatus.CONFLICT),
    UNAUTHORIZED(2008, "Chưa xác thực tài khoản", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(2009, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),

    // =========================================================
    // USER (3xxx)
    // =========================================================
    USER_NOT_FOUND(3001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    WRONG_CURRENT_PASSWORD(3002, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(3003, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(3004, "Tên người dùng phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(3005, "Mật khẩu phải có ít nhất 8 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(3006, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),

    // =========================================================
    // OTP (5xxx)
    // =========================================================
    OTP_INVALID(5001, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(5002, "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới", HttpStatus.BAD_REQUEST),
    OTP_INVALID_OR_EXPIRED(5003, "Mã OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_NOT_FOUND(5004, "Token xác thực không tồn tại", HttpStatus.NOT_FOUND),

    // =========================================================
    // ADMIN (4xxx)
    // =========================================================
    CANNOT_LOCK_ADMIN(4001, "Không thể khóa tài khoản Admin khác", HttpStatus.FORBIDDEN),

    // =========================================================
    // CATEGORY (6xxx)
    // =========================================================
    CATEGORY_NOT_FOUND(6001, "Danh mục không tồn tại", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS(6002, "Tên danh mục đã tồn tại", HttpStatus.CONFLICT),
    CATEGORY_SLUG_EXISTS(6003, "Đường dẫn (Slug) đã tồn tại", HttpStatus.CONFLICT),
    CATEGORY_HAS_CHILDREN(6004, "Không thể xóa thư mục đang có danh mục con", HttpStatus.BAD_REQUEST),

    // =========================================================
    // SHOP (7xxx)
    // =========================================================
    SHOP_NOT_FOUND(7001, "Cửa hàng không tồn tại", HttpStatus.NOT_FOUND),
    SHOP_ALREADY_EXISTS(7002, "Bạn đã đăng ký một cửa hàng rồi", HttpStatus.CONFLICT),
    SHOP_NAME_EXISTS(7003, "Tên cửa hàng này đã được sử dụng", HttpStatus.CONFLICT),
    
    // =========================================================
    // CONTENT (8xxx)
    // =========================================================
    CONTENT_VIOLATION(8001, "Hình ảnh của bạn vi phạm chính sách nội dung (AI phát hiện nội dung không phù hợp). Vui lòng chọn ảnh khác.", HttpStatus.BAD_REQUEST);


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
