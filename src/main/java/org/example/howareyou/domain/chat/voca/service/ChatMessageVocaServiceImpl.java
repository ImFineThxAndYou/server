package org.example.howareyou.domain.chat.voca.service;


import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.voca.dto.ChatMessageReadModel;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.member.repository.ChatMessageDocumentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageVocaServiceImpl implements ChatMessageVocaService {
    private final ChatMessageDocumentRepository repository;

    @Override
    public List<ChatMessageReadModel> getMessagesInRange(Instant start, Instant end) {
        return repository.findByMessageTimeBetween(start, end).stream()
                .map(this::toReadModel)
                .toList();
    }

    private ChatMessageReadModel toReadModel(ChatMessageDocument doc) {
        return ChatMessageReadModel.builder()
                .chatRoomUuid(doc.getChatRoomUuid())
                .sender(doc.getSender())
                .content(doc.getContent())
                .messageTime(doc.getMessageTime())
                .build();
    }
}