package org.example.howareyou.domain.translate.dto;

import lombok.Getter;

@Getter
public class TranslateRequestDto {
    String q; // message
    String source; //source lang
    String target; //target lang
}
