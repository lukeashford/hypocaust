package com.example.the_machine.service;

import com.example.the_machine.db.ThreadEntity;
import com.example.the_machine.dto.ThreadDto;
import com.example.the_machine.dto.ThreadViewDto;
import com.example.the_machine.mapper.ArtifactMapper;
import com.example.the_machine.mapper.MessageMapper;
import com.example.the_machine.mapper.RunMapper;
import com.example.the_machine.mapper.ThreadMapper;
import com.example.the_machine.repo.ArtifactRepository;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThreadService {

  private final ThreadRepository threadRepository;
  private final MessageRepository messageRepository;
  private final ArtifactRepository artifactRepository;
  private final RunRepository runRepository;
  private final ThreadMapper threadMapper;
  private final MessageMapper messageMapper;
  private final ArtifactMapper artifactMapper;
  private final RunMapper runMapper;

  @Transactional
  public ThreadDto createThread() {
    final var now = Instant.now();
    final var thread = ThreadEntity.builder()
        .lastActivityAt(now)
        .build();

    final var savedThread = threadRepository.save(thread);
    return threadMapper.toDto(savedThread);
  }

  @Transactional(readOnly = true)
  public ThreadViewDto getThreadView(UUID threadId) {
    final var thread = threadRepository.findById(threadId)
        .orElseThrow(() -> new RuntimeException("Thread not found: " + threadId));

    final var threadDto = threadMapper.toDto(thread);

    final var messages = messageRepository.findByThreadIdOrderByCreatedAt(threadId);
    final var messageDtos = messages.stream()
        .map(messageMapper::toDto)
        .toList();

    final var artifacts = artifactRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
    final var artifactDtos = artifacts.stream()
        .map(artifactMapper::toDto)
        .toList();

    var latestRun = runRepository.findTopByThreadIdOrderByStartedAtDesc(threadId);
    var latestRunDto = latestRun
        .map(runMapper::toDto)
        .orElse(null);

    return new ThreadViewDto(threadDto, messageDtos, artifactDtos, latestRunDto);
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
}