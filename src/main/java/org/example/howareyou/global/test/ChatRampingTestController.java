package org.example.howareyou.global.test;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.dto.CreateChatRoomResponse;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageResponse;
import org.example.howareyou.domain.chat.websocket.dto.CreateChatMessageRequest;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class ChatRampingTestController {

    private final ChatMessageService chatMessageService;
    private final MemberService memberService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;

    @Operation(
            summary = "부하테스트용 채팅 메시지 전송",
            description = """
        클라이언트가 WebSocket으로 /app/chat.rampingtest.send 에 메시지를 전송하면,
        서버는 membername 기반으로 senderId를 매핑하여 메시지를 저장하고,
        /topic/testchatroom/{chatRoomId}로 브로드캐스팅합니다.
        """
    )
    @MessageMapping("/chat.rampingtest.send")
    public void sendRampingTestMessage(@Valid @Payload CreateChatMessageRequest req) {
//        log.info("🚀 ChatRampingTestController.sendRampingTestMessage 호출됨! req={}", req);

        if (req.getMembername() == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Long memberId = memberService.getIdByMembername(req.getMembername());
        if (memberId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        req.setSenderId(memberId);

        try {
            ChatMessageResponse savedDoc = chatMessageService.saveTestMongo(req);
//            log.info("💾 [TEST] 메시지 저장 성공: messageId={}", savedDoc.getId());

            String destination = "/topic/testchatroom/" + savedDoc.getChatRoomUuid();
            messagingTemplate.convertAndSend(destination,savedDoc);
//            log.info("📢 [TEST] 메시지 브로드캐스트 완료: destination={}", destination);

        } catch (Exception e) {
            log.error("❌ [TEST] 메시지 처리 중 오류 발생", e);
            throw e;
        }
    }
    @PostMapping("/chat/force-create")
    public CreateChatRoomResponse forceCreateRoom(@RequestBody Map<String, String> body) {
        String sender = body.get("sender");
        String receiver = body.get("receiver");

        Long senderId = memberService.getIdByMembername(sender);
        Long receiverId = memberService.getIdByMembername(receiver);

        // 실제 createChatRoom 로직 안 쓰고, 테스트용 강제 채팅방 생성
        return chatRoomService.forceCreateChatRoom(senderId, receiverId);
    }

    //  전체 채팅방 UUID 조회
    @GetMapping("/chat/rooms")
    public List<String> getAllChatRoomUuids() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        return rooms.stream()
                .map(ChatRoom::getUuid)   // 엔티티에서 UUID getter
                .collect(Collectors.toList());
    }

    @GetMapping("/chat/rooms-with-members")
    public Map<String, List<String>> getAllChatRoomsWithMembers() {
        return chatRoomRepository.findAll().stream()
                .collect(Collectors.toMap(ChatRoom::getUuid,
                        r -> r.getMembers().stream()
                                .map(m -> m.getMember().getMembername())
                                .toList()));
    }
}