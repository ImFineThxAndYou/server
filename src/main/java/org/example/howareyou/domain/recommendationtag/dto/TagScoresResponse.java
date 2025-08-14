package org.example.howareyou.domain.recommendationtag.dto;

import java.util.Map;
import lombok.Getter;

@Getter
public class TagScoresResponse {
  private Map<String, Double> scores;

  public TagScoresResponse(Map<String, Double> scores) {
    this.scores = scores;
  }
}