package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VerificationToken;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.VerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.mail.username:noreply@gmail.com}")
    private String fromEmail;

    private static final int OTP_VALID_DURATION_MINUTES = 5;
    private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    @Transactional
    public void generateAndSendOtp(String email) {
        // Tìm user, không ném ngoại lệ để tránh lộ lọt tài khoản (Anti-enumeration)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Yêu cầu OTP cho email không tồn tại: {}", email);
            return; 
        }

        // Xóa các token reset cũ trước khi tạo mới
        verificationTokenRepository.deleteByUserAndType(user, TYPE_PASSWORD_RESET);

        String otp = generateNumericOtp(6);

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(otp)
                .type(TYPE_PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES))
                .isUsed(false)
                .build();
        
        verificationTokenRepository.save(token);

        sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    private String generateNumericOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "E-Commerce Market");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu");

            String htmlMsg = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333; max-width: 600px; margin: 0 auto;'>" +
                    "<h2 style='color: #4CAF50;'>Yêu cầu đặt lại mật khẩu</h2>" +
                    "<p>Xin chào <strong>" + fullName + "</strong>,</p>" +
                    "<p>Bạn vừa yêu cầu đặt lại mật khẩu. Vui lòng sử dụng mã OTP dưới đây:</p>" +
                    "<div style='background-color: #f4f4f4; border-radius: 5px; padding: 15px; text-align: center; margin: 20px 0;'>" +
                    "<h1 style='color: #333; margin: 0; letter-spacing: 5px; font-size: 32px;'>" + otp + "</h1>" +
                    "</div>" +
                    "<p style='color: #f44336; font-size: 14px;'><strong>Lưu ý:</strong> Mã có hiệu lực trong " + OTP_VALID_DURATION_MINUTES + " phút.</p>" +
                    "</div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Lỗi gửi mail đến {}: {}", toEmail, e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR);
        }
    }

    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        VerificationToken token = verificationTokenRepository
                .findByUserEmailAndTokenAndType(email, otp, TYPE_PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        if (token.isExpired() || token.getIsUsed()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Cập nhật mật khẩu - Chú ý dùng tên field đúng (password hoặc passwordHash tùy Entity)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu đã dùng và xóa để dọn dẹp
        token.setIsUsed(true);
        verificationTokenRepository.delete(token);

        // Ép đăng xuất tất cả thiết bị để đảm bảo an toàn sau khi đổi pass
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}