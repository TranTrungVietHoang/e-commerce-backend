package com.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload ảnh lên Cloudinary, trả về secure_url.
     *
     * @param file    File ảnh từ FE
     * @param folder  Thư mục lưu trữ (e.g. "avatars", "products")
     * @return URL công khai của ảnh (https://res.cloudinary.com/...)
     */
    public String uploadImage(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "allowed_formats", "jpg,jpeg,png,webp,gif",
                            "moderation", "aws_rek" // <--- Kích hoạt AI AWS Rekognition quét ảnh
                    )
            );

            // Kiểm tra trạng thái duyệt của AI (nếu Cloudinary trả về kết quả ngay lập tức)
            if (result.get("moderation") != null) {
                log.info("AI Moderation status: {}", result.get("moderation"));
                // Một số thiết lập Cloudinary có thể tự động trả về 'rejected' nếu cấu hình ngưỡng trên Dashboard
                Object moderation = result.get("moderation");
                if (moderation.toString().contains("status=rejected")) {
                    throw new AppException(ErrorCode.CONTENT_VIOLATION);
                }
            }

            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("moderation")) {
                throw new AppException(ErrorCode.CONTENT_VIOLATION);
            }
            throw e;
        }
    }

    /**
     * Xoá ảnh khỏi Cloudinary theo public_id.
     */
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for {}: {}", publicId, e.getMessage());
        }
    }
}
