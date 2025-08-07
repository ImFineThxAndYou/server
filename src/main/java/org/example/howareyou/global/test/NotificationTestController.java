package org.example.howareyou.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 알림 시스템 테스트용 컨트롤러
 * - SSE 연결 테스트
 * - 알림 발송 테스트
 * - 온라인 상태 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/notifications")
public class NotificationTestController {

    private final NotificationPushService notificationPushService;
    private final MemberService memberService;

    /**
     * 채팅 알림 발송 테스트
     */
    @PostMapping("/send-chat")
    public ResponseEntity<Map<String, Object>> sendChatNotification(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String receiverName = (String) request.get("receiverName");
            Long roomId = Long.valueOf(request.get("roomId").toString());
            Long senderId = Long.valueOf(request.get("senderId").toString());
            String preview = (String) request.get("preview");
            
            if (receiverName == null || roomId == null || senderId == null || preview == null) {
                result.put("success", false);
                result.put("message", "receiverName, roomId, senderId, preview가 모두 필요합니다.");
                return ResponseEntity.badRequest().body(result);
            }
            
            // receiverName으로 memberId 조회
            Long receiverId = memberService.getIdByMembername(receiverName);
            
            // 알림 발송
            notificationPushService.sendChatNotify(roomId, senderId, preview, receiverId);
            
            result.put("success", true);
            result.put("message", "채팅 알림이 발송되었습니다.");
            result.put("receiverId", receiverId);
            result.put("receiverName", receiverName);
            
            log.info("테스트 채팅 알림 발송: receiver={}, roomId={}, senderId={}", 
                    receiverName, roomId, senderId);
            
        } catch (Exception e) {
            log.error("채팅 알림 발송 실패", e);
            result.put("success", false);
            result.put("message", "알림 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 시스템 알림 발송 테스트
     */
    @PostMapping("/send-system")
    public ResponseEntity<Map<String, Object>> sendSystemNotification(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String receiverName = (String) request.get("receiverName");
            String message = (String) request.get("message");
            
            if (receiverName == null || message == null) {
                result.put("success", false);
                result.put("message", "receiverName, message가 모두 필요합니다.");
                return ResponseEntity.badRequest().body(result);
            }
            
            Long receiverId = memberService.getIdByMembername(receiverName);
            
            // 시스템 알림은 아직 구현되지 않았으므로 로그만 출력
            log.info("시스템 알림 테스트: receiver={}, message={}", receiverName, message);
            
            result.put("success", true);
            result.put("message", "시스템 알림 테스트 로그가 출력되었습니다.");
            result.put("receiverId", receiverId);
            result.put("receiverName", receiverName);
            
        } catch (Exception e) {
            log.error("시스템 알림 테스트 실패", e);
            result.put("success", false);
            result.put("message", "시스템 알림 테스트 중 오류가 발생했습니다: " + e.getMessage());
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
            // 실제 구현에서는 RedisEmitter의 isOnline 메서드를 사용해야 함
            // 여기서는 간단히 memberId만 반환
            
            result.put("success", true);
            result.put("memberId", memberId);
            result.put("memberName", memberName);
            result.put("message", "SSE 연결 상태를 확인하려면 실제 SSE 엔드포인트를 사용하세요.");
            
        } catch (Exception e) {
            log.error("연결 상태 확인 실패", e);
            result.put("success", false);
            result.put("message", "연결 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
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