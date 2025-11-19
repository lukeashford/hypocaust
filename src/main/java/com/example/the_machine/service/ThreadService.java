package com.example.the_machine.service;

import com.example.the_machine.db.ThreadEntity;
import com.example.the_machine.dto.ArtifactMetadataDto;
import com.example.the_machine.dto.ThreadDto;
import com.example.the_machine.mapper.ThreadMapper;
import com.example.the_machine.repo.ThreadRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThreadService {

  private final ThreadRepository threadRepository;
  private final ThreadMapper threadMapper;
  private final ArtifactService artifactService;

  @Transactional
  public ThreadDto createThread() {
    final var now = Instant.now();
    final var thread = ThreadEntity.builder()
        .lastActivityAt(now)
        .build();

    final var savedThread = threadRepository.save(thread);
    return threadMapper.toDto(savedThread);
  }

  @Transactional
  public UUID getOrCreateByLibrechatConversationId(String conversationId) {
    final var thread = threadRepository.findByLibrechatConversationId(conversationId)
        .orElseGet(() -> threadRepository.save(ThreadEntity.builder()
            .librechatConversationId(conversationId)
            .lastActivityAt(Instant.now())
            .build()));

    return thread.getId();
  }

  public List<ArtifactMetadataDto> listThreadArtifacts(UUID threadId) {
    return artifactService.getThreadArtifactMetadata(threadId);
  }
}