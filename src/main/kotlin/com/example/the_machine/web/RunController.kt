package com.example.the_machine.web

import com.example.the_machine.common.Routes
import com.example.the_machine.dto.CreateRunRequestDto
import com.example.the_machine.dto.RunDto
import com.example.the_machine.service.RunService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for handling run creation and management.
 */
@RestController
class RunController(
  private val runService: RunService
) {

  private val log = LoggerFactory.getLogger(RunController::class.java)

  /**
   * Creates a new run.
   *
   * @param request the create run request
   * @return the created run DTO
   */
  @PostMapping(Routes.RUNS)
  fun createRun(@RequestBody request: CreateRunRequestDto): ResponseEntity<RunDto> {
    log.info("Creating run for thread: {}", request.threadId)
    val run = runService.createRun(request)
    return ResponseEntity.ok(run)
  }
}