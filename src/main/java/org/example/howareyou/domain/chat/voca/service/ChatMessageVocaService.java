package org.example.howareyou.domain.chat.voca.service;

import org.example.howareyou.domain.chat.voca.dto.ChatMessageReadModel;

import java.time.Instant;
import java.util.List;

public interface ChatMessageVocaService {
    List<ChatMessageReadModel> getMessagesInRange(Instant start, Instant end);
}