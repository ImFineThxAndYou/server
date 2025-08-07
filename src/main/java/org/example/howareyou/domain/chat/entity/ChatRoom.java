package org.example.howareyou.domain.chat.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.global.config.BaseTime;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom extends BaseTime {

  @Id
  @GeneratedValue
  private Long id;

  @Column(unique = true, nullable = false)
  private String uuid;

// 1:1, 1:n
  @Enumerated(EnumType.STRING)
  private ChatRoomType type = ChatRoomType.ONE_TO_ONE;

// 채팅 요청 수락/거절/대기
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ChatRoomStatus status = ChatRoomStatus.PENDING;

 // 채팅멤버
  @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ChatRoomMember> members = new ArrayList<>();

  @Column(nullable = true)
  private String ws_id; // 웹소켓 id 저장.
  @PrePersist
  public void generateUuid() {
    if (this.uuid == null) {
      this.uuid = UUID.randomUUID().toString();
    }
  }

  // 참여자 유틸 메서드
  public List<Long> getParticipantIds() {
    return members.stream()
        .map(m -> m.getMember().getId())
        .toList();
  }

  public boolean hasParticipant(Long memberId) {
    return members.stream()
        .anyMatch(m -> m.getMember().getId().equals(memberId));
  }

  public Member getOtherParticipant(Long myId) {
    return members.stream()
        .map(ChatRoomMember::getMember)
        .filter(member -> !member.getId().equals(myId))
        .findFirst()
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
  }

  public void addMember(ChatRoomMember member) {
    members.add(member);
    member.setChatRoom(this);
  }
}
