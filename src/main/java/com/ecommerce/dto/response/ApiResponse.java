package com.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lớp bọc chuẩn cho MỌI API response trong hệ thống.
 * 
 * FE đọc field "code" để phân loại kết quả:
 *   200 → Thành công, đọc "result"
 *   4xx → Lỗi client, hiển thị "message"
 *   5xx → Lỗi server
 * 
 * @JsonInclude(NON_NULL): Không trả về field "result" nếu nó null
 *   → Response lỗi sẽ gọn hơn, không có "result: null"
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;       // HTTP-like status code (200, 400, 401...)
    private String message; // Thông báo hiển thị cho người dùng
    private T result;       // Dữ liệu thực tế (null nếu là response lỗi)

    /**
     * Factory method — tạo response thành công nhanh.
     * Ví dụ: return ApiResponse.success(userDto);
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Thành công")
                .result(data)
                .build();
    }

    /**
     * Factory method — tạo response thành công với message tùy chỉnh.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .result(data)
                .build();
    }
}
