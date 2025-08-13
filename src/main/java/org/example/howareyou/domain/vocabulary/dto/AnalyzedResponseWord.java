package org.example.howareyou.domain.vocabulary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedResponseWord {
    private String word;
    private String pos;
    private String lang;

    private String sourceMessageId;     //단어가 쓰인 채팅 ID
    private String example;             //단어가 쓰인 예문
}