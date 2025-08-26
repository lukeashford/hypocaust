package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.dto.CreateRunRequest;
import com.example.the_machine.dto.RunDTO;
import com.example.the_machine.service.RunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling run creation and management.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RunController {

  private final RunService runService;

  /**
   * Creates a new run.
   *
   * @param request the create run request
   * @return the created run DTO
   */
  @PostMapping(Routes.RUNS)
  public ResponseEntity<RunDTO> createRun(@RequestBody CreateRunRequest request) {
    log.info("Creating run for thread: {}", request.threadId());
    val run = runService.createRun(request);
    return ResponseEntity.ok(run);
  }
}