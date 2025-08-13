package org.example.howareyou.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.dto.ChatRoomResponse;
import org.example.howareyou.domain.chat.dto.ChatRoomSummaryResponse;
import org.example.howareyou.domain.chat.dto.CreateChatRoomRequest;
import org.example.howareyou.domain.chat.dto.CreateChatRoomResponse;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅방", description = "채팅방 생성, 수락/거절, 조회 등 관련 API")
@RequestMapping("/api/chat-room")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @Operation(summary = "채팅방 생성", description = "상대방과의 1:1 채팅방을 생성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "채팅방 생성 성공",
          content = @Content(schema = @Schema(implementation = CreateChatRoomResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping("/create")
  public CreateChatRoomResponse createChatRoom(
      @RequestBody CreateChatRoomRequest request,
      @Parameter(hidden = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    log.info("request 입니다 : {}", request);
    Long senderId = memberDetails.getId();
    return chatRoomService.createChatRoom(request, senderId);
  }

  @Operation(summary = "채팅방 수락", description = "상대방의 채팅 요청을 수락합니다.")
  @PostMapping("/{roomUuid}/accept")
  public void acceptChatRoom(
      @Parameter(description = "채팅방 UUID", required = true)
      @PathVariable String roomUuid,

      @Parameter(description = "수락하는 사용자 ID", required = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    chatRoomService.acceptChatRoom(roomUuid, memberDetails.getId());
  }

  @Operation(summary = "채팅방 거절", description = "상대방의 채팅 요청을 거절합니다.")
  @PostMapping("/{roomUuid}/reject")
  public void rejectChatRoom(
      @Parameter(description = "채팅방 UUID", required = true)
      @PathVariable String roomUuid,

      @Parameter(description = "거절하는 사용자 ID", required = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    chatRoomService.rejectChatRoom(roomUuid, memberDetails.getId());
  }

  @Operation(summary = "단일 채팅방 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "조회 성공",
      content = @Content(schema = @Schema(implementation = ChatRoomResponse.class)))
  @GetMapping("/{roomUuid}")
  public ChatRoomResponse getChatRoom(
      @Parameter(description = "채팅방 UUID", required = true)
      @PathVariable String roomUuid,

      @Parameter(hidden = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    Long myId = memberDetails.getId();
    return chatRoomService.getChatRoom(roomUuid, myId);
  }

  @Operation(summary = "내 채팅방 목록 조회", description = "현재 로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "조회 성공",
      content = @Content(schema = @Schema(implementation = ChatRoomSummaryResponse.class)))
  @GetMapping("/my-rooms")
  public List<ChatRoomSummaryResponse> getMyChatRooms(
      @Parameter(hidden = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    Long myId = memberDetails.getId();
    return chatRoomService.getMyChatRooms(myId);
  }

  @Operation(summary = "채팅방 연결 해제 및 삭제", description = "현재 사용자가 채팅방에서 나가고 연결을 끊습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "채팅방 없음")
  })
  @DeleteMapping("/{roomUuid}")
  public void disconnectChatRoom(
      @Parameter(description = "채팅방 UUID", required = true)
      @PathVariable String roomUuid,

      @Parameter(hidden = true)
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    Long myId = memberDetails.getId();
    chatRoomService.disconnectFromChatRoom(myId, roomUuid);
  }
}
