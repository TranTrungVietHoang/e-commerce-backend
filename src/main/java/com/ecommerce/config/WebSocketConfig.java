package com.ecommerce.config;

import com.ecommerce.security.JwtUtil;
import com.ecommerce.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Khai báo endpoint /ws. Frontend sẽ kết nối tới wss://domain/ws
        // withSockJS() tạo fallback nếu browser không hỗ trợ native websocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Tiền tố cho message Frontend gửi LÊN server
        registry.setApplicationDestinationPrefixes("/app");
        // /topic: Broadcast (tất cả mọi người), /user: Gửi đích danh riêng 1 người (Chat)
        registry.enableSimpleBroker("/topic", "/user");
        // Spring Boot tự xử lý /user/{username}/queue
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Xác thực JWT ngay từ khung truyền dữ liệu CONNECT đầu tiên của WebSocket.
     * Vì WebSocket thuần trong trình duyệt không cho phép gắn custom Headers,
     * nên frontend dùng thư viện STOMPjs để gửi Authorization Header trong frame CONNECT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = 
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                        
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    
                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (bearerToken.startsWith("Bearer ")) {
                            String token = bearerToken.substring(7);
                            try {
                                if (jwtUtil.validateToken(token)) {
                                    String username = jwtUtil.extractUsername(token);
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                    
                                    // Tạo Security context và gán vào session của WebSocket
                                    UsernamePasswordAuthenticationToken auth = 
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails, null, userDetails.getAuthorities());
                                    
                                    SecurityContextHolder.getContext().setAuthentication(auth);
                                    accessor.setUser(auth);
                                    log.info("WebSocket kết nối thành công: JWT Xác Thực cho user [{}]", username);
                                }
                            } catch (Exception e) {
                                log.error("Lỗi xác thực JWT của WebSocket", e);
                            }
                        }
                    } else {
                        log.warn("WebSocket CONNECT bị từ chối do thiếu Header Authorization");
                    }
                }
                return message;
            }
        });
    }
}
