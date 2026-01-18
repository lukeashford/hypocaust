package com.example.hypocaust.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hypocaust.service.events.EventService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private EventService eventService;

  @Test
  void testGetProjectLogs() throws Exception {
    UUID projectId = UUID.randomUUID();
    String mockLogs =
        "[2026-01-18T17:49:00Z] run.scheduled: RunScheduledEventPayload[runId=" + UUID.randomUUID()
            + "]";

    when(eventService.getProjectLogs(projectId)).thenReturn(mockLogs);

    mockMvc.perform(get("/projects/{id}/logs", projectId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/plain"))
        .andExpect(content().string(mockLogs));
  }
}
