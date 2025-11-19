package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.dto.ArtifactMetadataDto;
import com.example.the_machine.dto.ThreadDto;
import com.example.the_machine.service.ThreadService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Routes.THREADS)
@RequiredArgsConstructor
public class ThreadController {

  private final ThreadService threadService;

  @PostMapping
  public ResponseEntity<ThreadDto> createThread() {
    return ResponseEntity.status(HttpStatus.CREATED).body(threadService.createThread());
  }

  @GetMapping("/{threadId}/artifacts")
  public ResponseEntity<List<ArtifactMetadataDto>> listThreadArtifacts(
      @PathVariable UUID threadId) {
    return ResponseEntity.ok(threadService.listThreadArtifacts(threadId));
  }
}