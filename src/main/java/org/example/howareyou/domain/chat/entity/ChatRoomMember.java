package org.example.howareyou.domain.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.global.config.BaseTime;
import org.example.howareyou.global.entity.BaseEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomMember extends BaseTime {

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

  public ChatRoomMember(ChatRoom chatRoom, Member member, ChatRoomMemberStatus status) {
    this.chatRoom = chatRoom;
    this.member = member;
    this.status = status;
  }
}
