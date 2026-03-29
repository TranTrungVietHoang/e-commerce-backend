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
public class OtpService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int OTP_VALID_DURATION_MINUTES = 5;
    private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    @Transactional
    public void generateAndSendOtp(String email) {
        // Tìm user, không ném ngoại lệ nếu không tìm thấy để tránh lộ lọt tài khoản (Anti-enumeration)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        // Xoá các OTP cũ cùng loại
        verificationTokenRepository.deleteByUserAndType(user, TYPE_PASSWORD_RESET);

        // Sinh mã OTP ngẫu nhiên 6 số
        String otp = generateNumericOtp(6);

        // Lưu vào DB
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(otp)
                .type(TYPE_PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_DURATION_MINUTES))
                .build();
        
        verificationTokenRepository.save(token);

        // Gửi email
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
            helper.setSubject("Mã xác nhận đăt lại mật khẩu");

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

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR);
        }
    }

    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        VerificationToken token = verificationTokenRepository.findByUserEmailAndTokenAndType(email, otp, "PASSWORD_RESET")
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Đổi mật khẩu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xoá token đã sử dụng
        verificationTokenRepository.delete(token);

        // Xoá toàn bộ refresh token hiện có (ép đăng xuất tất cả thiết bị)
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
