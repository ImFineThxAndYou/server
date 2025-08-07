package org.example.howareyou.domain.chat.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.dto.*;
import org.example.howareyou.domain.chat.entity.*;
import org.example.howareyou.domain.chat.repository.ChatRoomMemberRepository;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageDocumentResponse;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
  /*  TODO
       1,2,3 은 나중에 고려하기.
  *    1. 내가 나갔을때 - 상대방한테 유지?삭제?
  *    2. 상대방이 나갔을때 - 나한테 유지?삭제?
  *    3. 아예 커넥션 끊기 (채팅방 ID 자체가 사라지는데 softdelete)
  *    -----------------------------------------------------
  *    4. 채팅방 시간기준 정렬
  */
  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final MemberRepository memberRepository;
  private final ChatMessageDocumentRepository chatMessageDocumentRepository;

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
  /* 채팅방 수락 */
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
        member.setJoinedAt(Instant.now());
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
          String uuid = room.getUuid();
          // 마지막 메시지 시간 조회 (없으면 Instant.EPOCH)
          Instant messageTime = getLastMessage(uuid)
                  .map(ChatMessageDocumentResponse::getMessageTime)
                  .orElse(Instant.EPOCH);
          return new ChatRoomSummaryResponse(
              uuid,
              opponent.getId(),
              opponent.getMembername(),
              room.getStatus().name(),
                  messageTime
          );
        })
            .sorted(Comparator.comparing(ChatRoomSummaryResponse::getMessageTime).reversed()) // 최신순 정렬
            .toList();
  }
  /* 추가: 시간기준 정렬 (최신채팅방 맨위로)*/
  public Optional<ChatMessageDocumentResponse> getLastMessage(String chatRoomUuid) {
    return chatMessageDocumentRepository
            .findLatestMessageByChatRoom(chatRoomUuid)
            .map(ChatMessageDocumentResponse::from);
  }

  /* 추가: 승인요청 받은 목록조회 */
  @Transactional(readOnly = true)
  public List<ChatRoomRequestMemberResponse> getRequestMembers(Long myId) {
    Member me = memberRepository.findById(myId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    // 내가 참여한 모든 채팅방 멤버 레코드
    List<ChatRoomMember> myEntries = chatRoomMemberRepository.findByMember(me);

    return myEntries.stream()
            .flatMap(myEntry -> {
              ChatRoom room = myEntry.getChatRoom();
              Instant myCreatedAt = myEntry.getJoinedAt();

              return room.getMembers().stream()
                      .filter(other -> !other.getMember().getId().equals(myId))
                      .filter(other -> other.getJoinedAt().isBefore(myCreatedAt))
                      .map(other -> new ChatRoomRequestMemberResponse(
                              other.getMember().getId(),
                              other.getMember().getMembername(),
                              room.getUuid(),
                              other.getStatus().name(),
                              other.getJoinedAt()
                      ));
            })
            .toList();
  }
}
