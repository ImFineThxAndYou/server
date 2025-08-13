package org.example.howareyou.domain.recommendationtag.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class ClassifyRequest {
  private List<String> words;

  public ClassifyRequest(List<String> words) {
    this.words = words;
  }
}

