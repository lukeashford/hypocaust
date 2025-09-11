package com.example.the_machine.web

import com.example.the_machine.common.Routes
import com.example.the_machine.dto.ThreadDto
import com.example.the_machine.dto.ThreadViewDto
import com.example.the_machine.service.ThreadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(Routes.THREADS)
class ThreadController(
  private val threadService: ThreadService
) {

  @PostMapping
  fun createThread(): ResponseEntity<ThreadDto> =
    ResponseEntity.status(HttpStatus.CREATED).body(threadService.createThread())

  @GetMapping("/{id}")
  fun getThread(@PathVariable id: UUID): ResponseEntity<ThreadViewDto> =
    ResponseEntity.ok(threadService.getThreadView(id))
}