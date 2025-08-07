package org.example.howareyou.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.entity.ChatRoomMember;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
  List<ChatRoomMember> findByMember(Member member);
  List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);
  void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);
  Optional<ChatRoomMember> findByChatRoomAndMemberId(ChatRoom chatRoom, Long memberId);

}
