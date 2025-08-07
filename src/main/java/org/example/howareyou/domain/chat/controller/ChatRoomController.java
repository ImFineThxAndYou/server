package org.example.howareyou.domain.chat.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.dto.ChatRoomResponse;
import org.example.howareyou.domain.chat.dto.ChatRoomSummaryResponse;
import org.example.howareyou.domain.chat.dto.CreateChatRoomRequest;
import org.example.howareyou.domain.chat.dto.CreateChatRoomResponse;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat-room")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  /**
   * 채팅방 생성 요청 (POST)
   */
  @PostMapping("/create")
  public CreateChatRoomResponse createChatRoom(@RequestBody CreateChatRoomRequest request,
      @RequestParam Long senderId) {
    return chatRoomService.createChatRoom(request, senderId);
  }

  /**
   * 채팅방 수락 (POST)
   */
  @PostMapping("/{roomUuid}/accept")
  public void acceptChatRoom(@PathVariable String roomUuid,
      @RequestParam Long receiverId) {
    chatRoomService.acceptChatRoom(roomUuid, receiverId);
  }

  /**
   * 채팅방 거절 (POST)
   */
  @PostMapping("/{roomUuid}/reject")
  public void rejectChatRoom(@PathVariable String roomUuid,
      @RequestParam Long receiverId) {
    chatRoomService.rejectChatRoom(roomUuid, receiverId);
  }


  /**
   * 단일 채팅방 상세 조회 (GET)
   */
  @GetMapping("/{roomUuid}")
  public ChatRoomResponse getChatRoom(@PathVariable String roomUuid,
      @RequestParam Long memberId) {
    return chatRoomService.getChatRoom(roomUuid, memberId);
  }

  /**
   * 현재 유저가 참여 중인 채팅방 목록 (GET)
   */
  @GetMapping("/my-rooms")
  public List<ChatRoomSummaryResponse> getMyChatRooms(@RequestParam Long myId) {
    return chatRoomService.getMyChatRooms(myId);
  }

  /**
   * 채팅방 연결 끊기 및 삭제 처리 (DELETE)
   */
  @DeleteMapping("/{roomUuid}")
  public void disconnectChatRoom(@PathVariable String roomUuid,
      @RequestParam Long memberId) {
    chatRoomService.disconnectFromChatRoom(memberId, roomUuid);
  }



}
