package org.example.howareyou.domain.vocabulary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzedResponseWord {
    private String text;
    private String pos;
    private String lang;
}