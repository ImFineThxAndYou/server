package org.example.howareyou.domain.chat.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Member;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomMember {

  @Id
  @GeneratedValue
  private Long id;
// 챗룸id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id")
  private ChatRoom chatRoom;
// 멤버 id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;
// 연결됐는지 안됐는지
  @Enumerated(EnumType.STRING)
  private ChatRoomMemberStatus status;
// 참여시각
  @Column(updatable = false)
  private Instant joinedAt;

  public ChatRoomMember(ChatRoom chatRoom, Member member, ChatRoomMemberStatus status) {
    this.chatRoom = chatRoom;
    this.member = member;
    this.status = status;
  }
}
