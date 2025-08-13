package org.example.howareyou.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.entity.ChatRoomMember;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByUuid(String uuid);

  @Query("""
    SELECT DISTINCT c
    FROM ChatRoom c
    JOIN c.members m1
    JOIN c.members m2
    WHERE m1.member.id = :member1Id
      AND m2.member.id = :member2Id
  """)
  ChatRoom findByMembers(@Param("member1Id") Long member1Id, @Param("member2Id") Long member2Id);


}
