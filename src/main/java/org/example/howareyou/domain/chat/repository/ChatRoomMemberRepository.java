package org.example.howareyou.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.entity.ChatRoomMember;
import org.example.howareyou.domain.chat.entity.ChatRoomMemberStatus;
import org.example.howareyou.domain.chat.entity.ChatRoomStatus;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
  List<ChatRoomMember> findByMember(Member member);
  List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

  void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);

  Optional<ChatRoomMember> findByChatRoomAndMemberId(ChatRoom chatRoom, Long memberId);

  @Query("""
      select m from ChatRoomMember m
      where m.member.id = :memberId
        and m.status = :status
        and m.chatRoom.status = :roomStatus
      order by m.chatRoom.createdAt desc
      """)
  List<ChatRoomMember> findByMemberIdAndStatusAndRoomStatusOrderByRoomCreatedDesc(
      @Param("memberId") Long memberId,
      @Param("status") ChatRoomMemberStatus status,
      @Param("roomStatus") ChatRoomStatus roomStatus
  );

  @Query("""
    select r from ChatRoom r
    join fetch r.members m
    join fetch m.member
    where r.uuid = :uuid
""")
  Optional<ChatRoom> findByUuidWithMembers(@Param("uuid") String uuid);


}
