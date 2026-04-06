package com.ecommerce.aspect;

import com.ecommerce.annotation.Audit;
import com.ecommerce.entity.AuditLog;
import com.ecommerce.entity.User;
import com.ecommerce.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditAnnotation)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Audit auditAnnotation, Object result) {
        recordLog(joinPoint, auditAnnotation, "SUCCESS");
    }

    @AfterThrowing(pointcut = "@annotation(auditAnnotation)", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Audit auditAnnotation, Exception exception) {
        recordLog(joinPoint, auditAnnotation, "FAILURE: " + exception.getMessage());
    }

    private void recordLog(JoinPoint joinPoint, Audit auditAnnotation, String status) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = "anonymous";
            Long userId = null;

            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                userEmail = user.getEmail();
                userId = user.getId();
            }

            // Lấy IP address của Client
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }

            // Lấy tham số đầu vào
            String details = getMethodArguments(joinPoint);

            // Tạo đối tượng AuditLog
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(auditAnnotation.action())
                    .details(details)
                    .status(status)
                    .ipAddress(ipAddress)
                    .build();

            auditLogService.saveLog(auditLog);
            
        } catch (Exception e) {
            log.error("Error while recording audit log: ", e);
        }
    }

    private String getMethodArguments(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof HttpServletRequest) continue;
                
                details.append(parameterNames[i]).append(": ");
                try {
                    details.append(objectMapper.writeValueAsString(args[i]));
                } catch (Exception e) {
                    details.append(args[i].toString());
                }
                if (i < args.length - 1) details.append(", ");
            }
            
            return details.toString();
        } catch (Exception e) {
            return "Unable to parse arguments";
        }
    }
}
