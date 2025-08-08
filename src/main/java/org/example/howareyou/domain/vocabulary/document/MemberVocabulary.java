package org.example.howareyou.domain.vocabulary.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "member_vocabulary")
public class MemberVocabulary {

    @Id
    private String id;

    private String membername;     // 사용자 ID
    private Instant createdAt;     // 생성 시각

    private List<UserWordEntry> words; // 단어 리스트

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWordEntry {

        private String word;    //원문
        private String meaning; //뜻
        private String pos;     //품사
        private String lang;    //원문 언어 en / ko
        private String level;   // 난이도 en은 A1~C1까지 ko는 A~C까지
        private String dictionaryType;  //en or ko

        private String chatRoomUuid;   // 단어가 분석된 채팅방
        private Instant analyzedAt;    // 분석된 시점

        private int frequency;         // 단어 등장 빈도
    }
}