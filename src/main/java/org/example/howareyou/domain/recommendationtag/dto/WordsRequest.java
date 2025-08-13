package org.example.howareyou.domain.recommendationtag.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;

@Getter
public class WordsRequest {
  private List<@NotEmpty String> words;

}
