package org.example.howareyou.domain.vocabulary.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregatedWordEntry {
    private String word;
    private String meaning;
    private String pos;
    private String lang;
    private String level;
    private Instant analyzedAt;
    private String chatRoomUuid;
    private List<String> chatMessageId;
    private List<String> example;
}