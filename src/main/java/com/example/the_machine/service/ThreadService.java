package com.example.the_machine.service;

import com.example.the_machine.common.IdGenerator;
import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.ThreadDTO;
import com.example.the_machine.dto.ThreadViewDTO;
import com.example.the_machine.repo.ArtifactRepository;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.mapping.ArtifactMapper;
import com.example.the_machine.service.mapping.MessageMapper;
import com.example.the_machine.service.mapping.RunMapper;
import com.example.the_machine.service.mapping.ThreadMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThreadService {

  private final ThreadRepository threadRepository;
  private final MessageRepository messageRepository;
  private final ArtifactRepository artifactRepository;
  private final RunRepository runRepository;
  private final IdGenerator idGenerator;
  private final ThreadMapper threadMapper;
  private final MessageMapper messageMapper;
  private final ArtifactMapper artifactMapper;
  private final RunMapper runMapper;

  @Transactional
  public ThreadDTO createThread() {
    val now = Instant.now();
    val thread = ThreadEntity.builder()
        .id(idGenerator.newId())
        .createdAt(now)
        .lastActivityAt(now)
        .build();

    val savedThread = threadRepository.save(thread);
    return threadMapper.toDto(savedThread);
  }

  @Transactional(readOnly = true)
  public ThreadViewDTO getThreadView(UUID threadId) {
    val thread = threadRepository.findById(threadId)
        .orElseThrow(() -> new RuntimeException("Thread not found: " + threadId));

    val threadDto = threadMapper.toDto(thread);

    val messages = messageRepository.findByThreadIdOrderByCreatedAt(threadId);
    val messageDtos = messages.stream()
        .map(messageMapper::toDto)
        .toList();

    val artifacts = artifactRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
    val artifactDtos = artifacts.stream()
        .map(artifactMapper::toDto)
        .toList();

    var latestRun = runRepository.findTopByThreadIdOrderByStartedAtDesc(threadId);
    var latestRunDto = latestRun
        .map(runMapper::toDto)
        .orElse(null);

    return new ThreadViewDTO(threadDto, messageDtos, artifactDtos, latestRunDto);
  }
}