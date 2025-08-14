package org.example.howareyou.domain.vocabulary.quiz;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VocaDTO {
    private String word;                // 단어
    private String meaning;             // 뜻
    private String pos;                 // 품사
    private String lang;                // 언어
    private String level;               // 난이도
    private String dictionaryType;      // 사전 유형 (ex: enko, koen)
    private Instant analyzedAt;         // 분석 시각
    private String chatRoomUuid;        // 채팅방 UUID
    private List<String> chatMessageId; // 해당 단어가 등장한 채팅 메시지 ID
    private List<String> example;       // 예문
}
