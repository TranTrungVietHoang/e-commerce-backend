package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VerificationToken;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.VerificationTokenRepository;
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
@Slf4j // Dùng để log lỗi chuyên nghiệp hơn System.err
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
        // Tìm user, nếu không thấy thì quăng lỗi thay vì lẳng lặng return
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
                    "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản của mình. Vui lòng sử dụng mã OTP gồm 6 chữ số dưới đây để tiếp tục:</p>" +
                    "<div style='background-color: #f4f4f4; border-radius: 5px; padding: 15px; text-align: center; margin: 20px 0;'>" +
                    "<h1 style='color: #333; margin: 0; letter-spacing: 5px; font-size: 32px;'>" + otp + "</h1>" +
                    "</div>" +
                    "<p style='color: #f44336; font-size: 14px;'><strong>Lưu ý:</strong> Mã này chỉ có hiệu lực trong " + OTP_VALID_DURATION_MINUTES + " phút.</p>" +
                    "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
                    "<hr style='border: 1px solid #eee; margin-top: 30px;'/>" +
                    "<p style='color: #777; font-size: 12px; text-align: center;'>Đội ngũ hỗ trợ E-Commerce Market</p>" +
                    "</div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.error("Lỗi gửi mail đến {}: {}", toEmail, e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Sửa lỗi: findByUserEmailAndTokenAndType trả về Optional nên phải dùng .orElseThrow
        VerificationToken token = verificationTokenRepository
                .findByUserEmailAndTokenAndType(email, otp, TYPE_PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        // Tận dụng hàm isExpired() đã viết ở Entity
        if (token.isExpired() || token.getIsUsed()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu token đã dùng hoặc xóa đi
        token.setIsUsed(true);
        verificationTokenRepository.save(token);

        // Xóa refresh token để bắt người dùng đăng nhập lại với mật khẩu mới (An toàn hơn)
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}