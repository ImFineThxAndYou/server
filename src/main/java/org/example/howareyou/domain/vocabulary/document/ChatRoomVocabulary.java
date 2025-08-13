package org.example.howareyou.domain.vocabulary.document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@Document(collection = "chatroom_vocabulary")
public class ChatRoomVocabulary {

    @Id
    private String id;

    private String chatRoomUuid;
    private Instant analyzedAt;
    private List<DictionaryWordEntry> words;

    @Getter
    @Setter
    @Builder
    public static class DictionaryWordEntry {
        private String word;
        private String meaning;
        private String pos;
        private String lang;
        private String level;
        private String dictionaryType;

        //단어가 사용된 문장
        private List<String> usedInMessages;

    }
}
