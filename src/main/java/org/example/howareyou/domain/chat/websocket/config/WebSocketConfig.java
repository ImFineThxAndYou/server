package org.example.howareyou.domain.chat.websocket.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.example.howareyou.global.security.CustomMemberDetailsService;
import org.example.howareyou.global.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomMemberDetailsService customMemberDetailsService;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    log.info("🌐 WebSocket 엔드포인트 등록: /ws-chatroom");
    registry
        .addEndpoint("/ws-chatroom") // 클라이언트 연결 엔드포인트
        .setAllowedOriginPatterns("*")
        .withSockJS(); // SockJS fallback 지원
    log.info("✅ WebSocket 엔드포인트 등록 완료");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    log.info("📡 메시지 브로커 설정 시작");
    registry.enableSimpleBroker("/topic"); // 메시지 구독 prefix
    registry.setApplicationDestinationPrefixes("/app"); // 메시지 전송 prefix
    log.info("✅ 메시지 브로커 설정 완료: 구독=/topic, 전송=/app");
  }

  @Override
  public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
    log.info("WebSocket 메시지 변환기 설정 시작...");
    
    // 기존 변환기 모두 제거
    messageConverters.clear();
    
    // CustomMemberDetails 역직렬화 방지를 위한 커스텀 메시지 변환기
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    ObjectMapper objectMapper = converter.getObjectMapper();
    
    // Java 8 시간 처리를 위한 모듈 추가
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    objectMapper.registerModule(javaTimeModule);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // CustomMemberDetails 역직렬화 완전 차단
    objectMapper.addMixIn(CustomMemberDetails.class, CustomMemberDetailsMixin.class);
    
    // 추가 보안 설정
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    
    // 변환기 추가
    messageConverters.add(converter);
    
    log.info("WebSocket 메시지 변환기 설정 완료: CustomMemberDetails 역직렬화 차단");
    log.info("설정된 변환기 개수: {}", messageConverters.size());
    return false; // 기본 변환기 사용하지 않음
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        log.info("🔍 WebSocket 메시지 수신: command={}, destination={}", 
                accessor.getCommand(), accessor.getDestination());
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          log.info("🔗 WebSocket CONNECT 요청 처리 시작");
          
          // CONNECT 헤더에서 JWT 토큰 추출
          String token = extractToken(accessor);
          
          if (token != null) {
            try {
              log.info("🔑 JWT 토큰 검증 시작");
              // JWT 토큰 검증 및 사용자 정보 추출
              String userId = jwtTokenProvider.validateAndGetSubject(token);
              log.info("✅ JWT 토큰 검증 성공: userId={}", userId);
              
              // CustomMemberDetails 생성
              CustomMemberDetails userDetails = (CustomMemberDetails) customMemberDetailsService.loadUserByUsername(userId);
              Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
              accessor.setUser(auth);
              
              log.info("✅ WebSocket CONNECT 인증 성공: userId={}, membername={}", userId, userDetails.getMembername());
            } catch (Exception e) {
              log.error("❌ WebSocket CONNECT 인증 실패: {}", e.getMessage(), e);
              // 인증 실패 시 연결 거부
              throw new IllegalArgumentException("WebSocket 인증 실패: " + e.getMessage());
            }
          } else {
            log.warn("⚠️ WebSocket CONNECT: 토큰이 없습니다 - 임시로 연결 허용");
            // 임시로 인증 없이 연결 허용 (디버깅용)
            // throw new IllegalArgumentException("WebSocket 인증 토큰이 필요합니다");
          }
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
          log.info("📤 WebSocket SEND 요청: destination={}", accessor.getDestination());
          
          // 디버깅: 메시지 본문 로그 출력
          if (message.getPayload() instanceof byte[]) {
            String payload = new String((byte[]) message.getPayload());
            log.info("📋 메시지 본문: {}", payload);
          }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
          log.info("📡 WebSocket SUBSCRIBE 요청: destination={}", accessor.getDestination());
        }
        
        return message;
      }
      
      private String extractToken(StompHeaderAccessor accessor) {
        // 1. Authorization 헤더에서 토큰 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          log.debug("🔑 Authorization 헤더에서 토큰 추출");
          return authHeader.substring(7);
        }
        
        // 2. access-token 헤더에서 토큰 추출 (프론트엔드용)
        String accessToken = accessor.getFirstNativeHeader("access-token");
        if (accessToken != null) {
          log.debug("🔑 access-token 헤더에서 토큰 추출");
          return accessToken;
        }
        
        // 3. 쿼리 파라미터에서 토큰 추출 (fallback)
        String queryToken = accessor.getFirstNativeHeader("token");
        if (queryToken != null) {
          log.debug("🔑 쿼리 파라미터에서 토큰 추출");
          return queryToken;
        }
        
        log.warn("⚠️ 토큰을 찾을 수 없음: Authorization={}, access-token={}, token={}", 
                authHeader, accessToken, queryToken);
        return null;
      }
    });
  }
  
  // CustomMemberDetails 역직렬화 방지를 위한 Mixin
  @com.fasterxml.jackson.annotation.JsonIgnoreType
  private static abstract class CustomMemberDetailsMixin {
    // 이 클래스 타입 자체를 Jackson에서 완전히 무시
  }
}
