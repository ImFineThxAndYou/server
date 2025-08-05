package org.example.howareyou.domain.chat.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.dto.ChatRoomResponse;
import org.example.howareyou.domain.chat.dto.ChatRoomSummaryResponse;
import org.example.howareyou.domain.chat.dto.CreateChatRoomRequest;
import org.example.howareyou.domain.chat.dto.CreateChatRoomResponse;
import org.example.howareyou.domain.chat.entity.*;
import org.example.howareyou.domain.chat.repository.ChatRoomMemberRepository;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final MemberRepository memberRepository;

  /* 채팅방 생성 */
  @Transactional
  public CreateChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long senderId) {
    Long receiverId = request.getReceiverId();

    Member sender = memberRepository.findById(senderId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    Member receiver = memberRepository.findById(receiverId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    // 이미 둘 사이에 존재하는 채팅방 확인
    ChatRoom existingRoom = chatRoomRepository.findByMembers(sender, receiver);
    if (existingRoom != null) {
      return new CreateChatRoomResponse(existingRoom.getUuid());
    }

    // 새로운 채팅방 생성
    ChatRoom chatRoom = new ChatRoom();
    chatRoom.setStatus(ChatRoomStatus.PENDING);
    chatRoomRepository.save(chatRoom);

    // 참여자 추가 (초기 상태는 PENDING)
    ChatRoomMember senderEntry = new ChatRoomMember(chatRoom, sender, ChatRoomMemberStatus.PENDING);
    ChatRoomMember receiverEntry = new ChatRoomMember(chatRoom, receiver, ChatRoomMemberStatus.PENDING);

    chatRoomMemberRepository.save(senderEntry);
    chatRoomMemberRepository.save(receiverEntry);

    return new CreateChatRoomResponse(chatRoom.getUuid());
  }

  @Transactional
  public void acceptChatRoom(String roomUuid, Long receiverId) {
    ChatRoom chatRoom = chatRoomRepository.findByUuid(roomUuid)
        .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    // 수락자 확인
    memberRepository.findById(receiverId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    // ChatRoomMember 2명 상태 확인 및 변경
    List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoom(chatRoom);

    for (ChatRoomMember member : members) {
      if (member.getStatus() != ChatRoomMemberStatus.JOINED) {
        member.setStatus(ChatRoomMemberStatus.JOINED);
        member.setJoinedAt(LocalDateTime.now());
      }
    }

    // 채팅방 전체 상태도 변경
    chatRoom.setStatus(ChatRoomStatus.ACCEPTED);
  }

  /* uuid 채팅방 단 건 조회 */
  @Transactional
  public ChatRoomResponse getChatRoom(String uuid, Long myId) {
    ChatRoom chatRoom = chatRoomRepository.findByUuid(uuid)
        .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    if (!chatRoom.hasParticipant(myId)) {
      throw new CustomException(ErrorCode.FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    Member opponent = chatRoom.getOtherParticipant(myId);

    return new ChatRoomResponse(
        chatRoom.getUuid(),
        chatRoom.getStatus().name(),
        opponent.getId(),
        opponent.getMembername()
    );
  }

  /* 내가 참여 중인 채팅방 목록 조회 */
  @Transactional
  public List<ChatRoomSummaryResponse> getMyChatRooms(Long myId) {
    Member me = memberRepository.findById(myId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    List<ChatRoomMember> myEntries = chatRoomMemberRepository.findByMember(me);

    return myEntries.stream()
        .map(entry -> {
          ChatRoom room = entry.getChatRoom();
          Member opponent = room.getOtherParticipant(myId);
          return new ChatRoomSummaryResponse(
              room.getUuid(),
              opponent.getId(),
              opponent.getMembername(),
              room.getStatus().name()
          );
        })
        .toList();
  }
}
