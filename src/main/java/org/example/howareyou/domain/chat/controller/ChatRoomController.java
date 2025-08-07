package org.example.howareyou.domain.chat.controller;


import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.dto.*;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatroom")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    /* 채팅방 생성 */
    @PostMapping
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(
            @RequestBody CreateChatRoomRequest request,
            @RequestParam Long senderId // 인증 후면 생략 가능
    ) {
        CreateChatRoomResponse response = chatRoomService.createChatRoom(request, senderId);
        return ResponseEntity.ok(response);
    }

    /* 채팅방 수락*/
    @PatchMapping("/room/{uuid}/accept")
    public ResponseEntity<Void> acceptChatRoom(
            @PathVariable String uuid,
            @RequestParam Long receiverId // 인증 후면 생략 가능
    ) {
        chatRoomService.acceptChatRoom(uuid, receiverId);
        return ResponseEntity.ok().build();
    }

    // 채팅방 단건 조회
    @GetMapping("/room/{uuid}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable String uuid,
            @RequestParam Long myId
    ) {
        ChatRoomResponse response = chatRoomService.getChatRoom(uuid, myId);
        return ResponseEntity.ok(response);
    }

    // 내가 참여 중인 채팅방 리스트
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms(
            @RequestParam Long myId
    ) {
        List<ChatRoomSummaryResponse> responses = chatRoomService.getMyChatRooms(myId);
        return ResponseEntity.ok(responses);
    }


}
