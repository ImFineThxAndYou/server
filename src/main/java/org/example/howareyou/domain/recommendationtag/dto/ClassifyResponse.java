package org.example.howareyou.domain.recommendationtag.dto;

import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassifyResponse {
  private Map<String, Double> scores;
}