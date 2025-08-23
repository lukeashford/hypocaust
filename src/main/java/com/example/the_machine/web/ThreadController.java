package com.example.the_machine.web;

import com.example.the_machine.dto.ThreadDTO;
import com.example.the_machine.dto.ThreadViewDTO;
import com.example.the_machine.service.ThreadService;
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
@RequestMapping("/threads")
@RequiredArgsConstructor
public class ThreadController {

  private final ThreadService threadService;

  @PostMapping
  public ResponseEntity<ThreadDTO> createThread() {
    return ResponseEntity.status(HttpStatus.CREATED).body(threadService.createThread());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ThreadViewDTO> getThread(@PathVariable UUID id) {
    return ResponseEntity.ok(threadService.getThreadView(id));
  }
}