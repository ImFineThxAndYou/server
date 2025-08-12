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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id")
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private ChatRoomMemberStatus status;

  @Column(updatable = false)
  private Instant joinedAt;

  public ChatRoomMember(ChatRoom chatRoom, Member member, ChatRoomMemberStatus status) {
    this.chatRoom = chatRoom;
    this.member = member;
    this.status = status;
  }
}
