package org.example.howareyou.global.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.notification.dto.NotifyDto;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.example.howareyou.domain.notification.service.NotificationService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 알림 시스템 테스트용 컨트롤러
 * 
 * 제공 기능:
 * - 채팅 알림 발송 테스트
 * - 채팅 요청 알림 발송 테스트
 * - 시스템 알림 발송 테스트
 * - 알림 목록 조회 테스트
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/notifications")
@Tag(name = "알림 테스트", description = "알림 시스템 테스트용 API (개발 환경에서만 사용)")
public class NotificationTestController {

    private final NotificationPushService notificationPushService;
    private final MemberService memberService;
    private final org.example.howareyou.domain.notification.redis.RedisEmitter emitters;

    @Operation(
        summary = "채팅 알림 발송 테스트",
        description = "채팅 메시지 알림을 특정 사용자에게 발송합니다. " +
                     "receiverName, roomId, senderId, messageId, message가 모두 필요합니다."
    )
    @PostMapping("/send-chat")
    public ResponseEntity<Map<String, Object>> sendChatNotification(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String receiverName = (String) request.get("receiverName");
            Long roomId = Long.valueOf(request.get("roomId").toString());
            Long senderId = Long.valueOf(request.get("senderId").toString());
            String messageId = (String) request.get("messageId");
            String message = (String) request.get("message");
            
            if (receiverName == null || roomId == null || senderId == null || messageId== null|| message== null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, 
                        "receiverName, roomId, senderId, preview가 모두 필요합니다.1");
            }
            
            // receiverName으로 memberId 조회
            Long receiverId = memberService.getIdByMembername(receiverName);
            if (receiverId == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND, 
                        String.format("수신자를 찾을 수 없습니다: receiverName=%s", receiverName));
            }
            
            // 알림 발송
            notificationPushService.sendChatNotify(roomId, senderId, messageId, message, receiverId);
            
            result.put("success", true);
            result.put("message", "채팅 알림이 발송되었습니다.");
            result.put("receiverId", receiverId);
            result.put("receiverName", receiverName);
            
            log.info("테스트 채팅 알림 발송: receiver={}, roomId={}, senderId={}", 
                    receiverName, roomId, senderId);
            
        } catch (CustomException e) {
            log.warn("채팅 알림 발송 실패 (CustomException): {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("errorCode", e.getErrorCode().getCode());
        } catch (Exception e) {
            log.error("채팅 알림 발송 실패 (Unexpected)", e);
            result.put("success", false);
            result.put("message", "알림 발송 중 예상치 못한 오류가 발생했습니다.");
            result.put("errorCode", "UNEXPECTED_ERROR");
        }
        
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "채팅 요청 알림 발송 테스트",
        description = "채팅 요청 알림을 특정 사용자에게 발송합니다. " +
                     "receiverName, requesterId, requesterName, message가 모두 필요합니다."
    )
    @PostMapping("/send-chatreq")
    public ResponseEntity<Map<String, Object>> sendChatReqNotification(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String receiverName = (String) request.get("receiverName");
            Long requesterId = Long.valueOf(request.get("requesterId").toString());
            String requesterName = (String) request.get("requesterName");
            String message = (String) request.get("message");
            
            if (receiverName == null || requesterId == null || requesterName == null || message == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, 
                        "receiverName, requesterId, requesterName, message가 모두 필요합니다.");
            }
            
            Long receiverId = memberService.getIdByMembername(receiverName);
            if (receiverId == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND, 
                        String.format("수신자를 찾을 수 없습니다: receiverName=%s", receiverName));
            }
            
            // 채팅 요청 알림 발송
            notificationPushService.sendChatReqNotify(requesterId, requesterName, message, receiverId);
            
            result.put("success", true);
            result.put("message", "채팅 요청 알림이 발송되었습니다.");
            result.put("receiverId", receiverId);
            result.put("receiverName", receiverName);
            result.put("requesterId", requesterId);
            result.put("requesterName", requesterName);
            
            log.info("테스트 채팅 요청 알림 발송: receiver={}, requester={}, message={}", 
                    receiverName, requesterName, message);
            
        } catch (CustomException e) {
            log.warn("채팅 요청 알림 발송 실패 (CustomException): {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("errorCode", e.getErrorCode().getCode());
        } catch (Exception e) {
            log.error("채팅 요청 알림 발송 실패 (Unexpected)", e);
            result.put("success", false);
            result.put("message", "채팅 요청 알림 발송 중 예상치 못한 오류가 발생했습니다.");
            result.put("errorCode", "UNEXPECTED_ERROR");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 시스템 알림 발송 테스트 (공지사항/이벤트)
     */
    @PostMapping("/send-system")
    public ResponseEntity<Map<String, Object>> sendSystemNotification(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String receiverName = (String) request.get("receiverName");
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            String category = (String) request.get("category");
            
            if (receiverName == null || title == null || content == null || category == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, 
                        "receiverName, title, content, category가 모두 필요합니다.");
            }
            
            Long receiverId = memberService.getIdByMembername(receiverName);
            if (receiverId == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND, 
                        String.format("수신자를 찾을 수 없습니다: receiverName=%s", receiverName));
            }
            
            // 시스템 알림 발송
            notificationPushService.sendSystemNotify(title, content, category, receiverId);
            
            result.put("success", true);
            result.put("message", "시스템 알림이 발송되었습니다.");
            result.put("receiverId", receiverId);
            result.put("receiverName", receiverName);
            result.put("title", title);
            result.put("category", category);
            
            log.info("테스트 시스템 알림 발송: receiver={}, title={}, category={}", 
                    receiverName, title, category);
            
        } catch (CustomException e) {
            log.warn("시스템 알림 발송 실패 (CustomException): {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("errorCode", e.getErrorCode().getCode());
        } catch (Exception e) {
            log.error("시스템 알림 발송 실패 (Unexpected)", e);
            result.put("success", false);
            result.put("message", "시스템 알림 발송 중 예상치 못한 오류가 발생했습니다.");
            result.put("errorCode", "UNEXPECTED_ERROR");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * SSE 연결 상태 확인
     */
    @GetMapping("/status/{memberName}")
    public ResponseEntity<Map<String, Object>> checkConnectionStatus(
            @PathVariable String memberName) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long memberId = memberService.getIdByMembername(memberName);
            
            // RedisEmitter에서 온라인 상태 확인
            boolean isOnlineInRedis = emitters.isOnline(memberId);
            boolean hasLocalEmitter = emitters.get(memberId).isPresent();
            
            result.put("success", true);
            result.put("memberId", memberId);
            result.put("memberName", memberName);
            result.put("isOnlineInRedis", isOnlineInRedis);
            result.put("hasLocalEmitter", hasLocalEmitter);
            result.put("status", isOnlineInRedis && hasLocalEmitter ? "CONNECTED" : 
                               isOnlineInRedis ? "REDIS_ONLY" : "OFFLINE");
            
            log.info("SSE 상태 확인: memberId={}, redis={}, local={}", 
                    memberId, isOnlineInRedis, hasLocalEmitter);
            
        } catch (Exception e) {
            log.error("연결 상태 확인 실패", e);
            result.put("success", false);
            result.put("message", "연결 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 모든 SSE 연결 상태 확인 (디버깅용)
     */
    @GetMapping("/status-all")
    public ResponseEntity<Map<String, Object>> checkAllConnectionStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Redis에서 모든 온라인 사용자 확인
            // 실제로는 Redis에서 모든 sse:online:* 키를 조회해야 함
            // 여기서는 간단히 테스트용 사용자들만 확인
            
            String[] testUsers = {"test", "admin", "user1"};
            Map<String, Object> userStatuses = new HashMap<>();
            
            for (String username : testUsers) {
                try {
                    Long memberId = memberService.getIdByMembername(username);
                    boolean isOnlineInRedis = emitters.isOnline(memberId);
                    boolean hasLocalEmitter = emitters.get(memberId).isPresent();
                    
                    Map<String, Object> userStatus = new HashMap<>();
                    userStatus.put("memberId", memberId);
                    userStatus.put("isOnlineInRedis", isOnlineInRedis);
                    userStatus.put("hasLocalEmitter", hasLocalEmitter);
                    userStatus.put("status", isOnlineInRedis && hasLocalEmitter ? "CONNECTED" : 
                                         isOnlineInRedis ? "REDIS_ONLY" : "OFFLINE");
                    
                    userStatuses.put(username, userStatus);
                } catch (Exception e) {
                    userStatuses.put(username, Map.of("error", e.getMessage()));
                }
            }
            
            result.put("success", true);
            result.put("userStatuses", userStatuses);
            
        } catch (Exception e) {
            log.error("전체 연결 상태 확인 실패", e);
            result.put("success", false);
            result.put("message", "전체 연결 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 테스트용 사용자 목록 조회
     */
    @GetMapping("/test-users")
    public ResponseEntity<Map<String, Object>> getTestUsers() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 실제로는 MemberService에서 테스트 사용자 목록을 조회해야 함
            // 여기서는 하드코딩된 예시
            Map<String, Object> users = new HashMap<>();
            users.put("testuser1", "테스트 사용자 1");
            users.put("testuser2", "테스트 사용자 2");
            users.put("admin", "관리자");
            users.put("user1", "일반 사용자 1");
            users.put("user2", "일반 사용자 2");
            users.put("developer", "개발자");
            
            result.put("success", true);
            result.put("users", users);
            result.put("message", "테스트용 사용자 목록입니다.");
            
        } catch (Exception e) {
            log.error("테스트 사용자 목록 조회 실패", e);
            result.put("success", false);
            result.put("message", "테스트 사용자 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }


} 