package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.domain.RequestContext;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.service.ArtifactPanelService;
import com.example.the_machine.service.CentralChatService;
import com.example.the_machine.service.RunService;
import com.example.the_machine.service.ThreadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class OpenAiCompatController {

  private final CentralChatService chat;
  private final ObjectMapper mapper;
  private final RequestContext requestContext;
  private final ThreadService threadService;
  private final ArtifactPanelService artifactPanelService;
  private final RunService runService;

  @PostMapping(
      value = Routes.COMPLETIONS,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {
          MediaType.APPLICATION_JSON_VALUE,
          MediaType.TEXT_EVENT_STREAM_VALUE
      }
  )
  public Object completions(@RequestBody ChatCompletionRequest req) {
    final UUID threadId = threadService.getOrCreateByLibrechatConversationId(
        requestContext.librechatConversationId()
    );

    if (Boolean.TRUE.equals(req.stream())) {
      final var emitter = new SseEmitter(0L); // no timeout for long generations
      emitter.onTimeout(emitter::complete);
      CompletableFuture.runAsync(
          () -> stream(req, emitter, threadId)
      );
      return emitter; // Spring will set text/event-stream for SseEmitter
    }

    // non-stream: JSON response compatible with OpenAI
    var cr = chat.chatCompletion(req, threadId);
    var content = extractText(cr);

    ObjectNode body = mapper.createObjectNode()
        .put("id", "chatcmpl_" + UUID.randomUUID())
        .put("object", "chat.completion")
        .put("created", Instant.now().getEpochSecond())
        .put("model", req.model() == null ? "unknown" : req.model());

    var ch = body.putArray("choices").addObject();
    ch.put("index", 0);
    ch.putObject("message")
        .put("role", "assistant")
        .put("content", content);
    ch.put("finish_reason", "stop");

    // Optional usage placeholder
    body.putObject("usage");

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  private void stream(ChatCompletionRequest req, SseEmitter em, UUID threadId) {
    final String id = "chatcmpl_" + UUID.randomUUID();
    final long created = Instant.now().getEpochSecond();
    final String model = req.model() == null ? "unknown" : req.model();

    try {
      // Initial role delta (OpenAI-compatible)
      sendChunk(em, id, created, model, "assistant", null);

      var artifact = artifactPanelService.loadRunMonitor(threadId);
      sendChunk(em, id, created, model, null, artifact);
      runService.scheduleRun(new CreateRunRequestDto(threadId, "Bla"));

//      chat.streamChatCompletion(req, threadId).toStream().forEach(cr -> {
//        String delta = extractText(cr);
//        if (delta != null && !delta.isBlank()) {
//          sendChunk(em, id, created, model, null, delta);
//        }
//      });

      // Finish chunk then DONE sentinel
      sendFinish(em, id, created, model);
      em.send(SseEmitter.event().data("[DONE]"));
      em.complete();
    } catch (Exception e) {
      try {
        ObjectNode err = mapper.createObjectNode()
            .put("error", "stream_failed")
            .put("message", e.getMessage() == null ? "unknown" : e.getMessage());
        em.send(SseEmitter.event().data(err));
      } catch (Exception ignore) {
        // ignore secondary failures
      }
      em.completeWithError(e);
    }
  }

  private void sendChunk(SseEmitter em, String id, long created, String model, String role,
      String content) {
    ObjectNode root = mapper.createObjectNode()
        .put("id", id)
        .put("object", "chat.completion.chunk")
        .put("created", created)
        .put("model", model);

    var choice = root.putArray("choices").addObject().put("index", 0);
    var delta = choice.putObject("delta");
    if (role != null) {
      delta.put("role", role);
    }
    if (content != null) {
      delta.put("content", content);
    }
    choice.putNull("finish_reason");

    try {
      // Let Spring frame as "data: <json>\n\n"
      em.send(SseEmitter.event().data(root));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void sendFinish(SseEmitter em, String id, long created, String model) {
    ObjectNode root = mapper.createObjectNode()
        .put("id", id)
        .put("object", "chat.completion.chunk")
        .put("created", created)
        .put("model", model);

    root.putArray("choices").addObject()
        .put("index", 0)
        .putNull("delta")
        .put("finish_reason", "stop");

    try {
      em.send(SseEmitter.event().data(root));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String extractText(ChatResponse cr) {
    return cr.getResult().getOutput().getText();
  }
}
