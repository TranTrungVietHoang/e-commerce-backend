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

    /** Thành công với message mặc định */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Thành công")
                .result(data)
                .build();
    }

    /** Thành công với message tùy chỉnh */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .result(data)
                .build();
    }

    /** Tạo resource mới thành công (201) */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .code(201)
                .message("Tạo thành công")
                .result(data)
                .build();
    }

    /** Response lỗi với code và message tùy chỉnh */
    public static <T> ApiResponse<T> error(int code, String msg) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(msg)
                .result(null)
                .build();
    }
}
