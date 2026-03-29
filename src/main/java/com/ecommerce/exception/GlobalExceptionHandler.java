package com.ecommerce.exception;

import com.ecommerce.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý lỗi tập trung cho toàn bộ hệ thống.
 *
 * Mọi exception từ Controller/Service đều được bắt tại đây,
 * đảm bảo FE luôn nhận được cùng một cấu trúc ApiResponse.
 *
 * THỨ TỰ BẮT LỖI (từ cụ thể → chung):
 *   1. MethodArgumentNotValidException (validation @Valid)
 *   2. AppException (lỗi nghiệp vụ tùy chỉnh)
 *   3. AccessDeniedException (403 - Bảo mật Spring Security)
 *   4. AuthenticationException (401 - Xác thực thất bại)
 *   5. Exception (fallback - lỗi không lường trước)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi validation từ @Valid trên RequestBody.
     * Trả về danh sách tất cả field bị lỗi để FE hiển thị.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Dữ liệu đầu vào không hợp lệ")
                .result(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Bắt lỗi nghiệp vụ từ AppException.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(null)
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Bắt lỗi quyền truy cập (403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.ACCESS_DENIED.getCode())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .result(null)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Bắt lỗi xác thực Spring Security (401 Unauthorized).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_TOKEN.getCode())
                .message(ex.getMessage())
                .result(null)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Fallback: bắt mọi lỗi không lường trước được.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        ex.printStackTrace();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.UNCATEGORIZED_ERROR.getCode())
                .message("Lỗi hệ thống. Vui lòng thử lại sau")
                .result(null)
                .build();

        return ResponseEntity.internalServerError().body(response);
    }
}
