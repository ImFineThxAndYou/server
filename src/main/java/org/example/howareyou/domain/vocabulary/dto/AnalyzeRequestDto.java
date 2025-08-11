package org.example.howareyou.domain.vocabulary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeRequestDto {
    private String text;
}