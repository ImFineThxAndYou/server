package org.example.howareyou.domain.vocabulary.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "dictionary_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictionaryData {
    @Id
    private String id;
    //원문 단어 (enko -> 영어 / koen -> 한국어)
    private String word;
    //단어 뜻 (enko -> 한국어 / koen -> 영어)
    private String meaning;
    //품사 (enko -> 영어 / koen -> 한국어)
    private String pos;
    //단어 레벨 (enko -> A1~C1 / koen -> A~C)
    private String level;
    //(enko -> 영한 / koen -> 한영)
    private String dictionaryType;
    //생성일
    private Instant createdAt;
}