package org.example.howareyou.domain.vocabulary.service.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabularyCsvRow {
    @CsvBindByName
    private String word;

    @CsvBindByName
    private String meaning;

    @CsvBindByName
    private String pos;

    @CsvBindByName
    private String level;

    @CsvBindByName
    private String dictionaryType;
}