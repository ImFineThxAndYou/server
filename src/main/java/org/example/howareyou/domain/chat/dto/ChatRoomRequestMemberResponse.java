package org.example.howareyou.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatRoomRequestMemberResponse {
    private Long memberId;
    private String memberName;
    private String uuid;
    private String requestStatus; // 상대방의 참여상태
    private Instant createAt; //채팅 생성시각 (누가 요청보냈는지 확인용도)
}
