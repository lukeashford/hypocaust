package com.example.hypocaust.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.dto.ProjectResponseDto;
import com.example.hypocaust.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProjectService projectService;

  @Test
  void shouldListAllProjects() throws Exception {
    // Given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    ProjectResponseDto dto1 = new ProjectResponseDto(id1, "Project 1");
    ProjectResponseDto dto2 = new ProjectResponseDto(id2, "Project 2");

    when(projectService.getProjects()).thenReturn(List.of(dto1, dto2));

    // When & Then
    mockMvc.perform(get(Routes.PROJECTS).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Project 1"))
        .andExpect(jsonPath("$[0].id").value(id1.toString()))
        .andExpect(jsonPath("$[1].name").value("Project 2"))
        .andExpect(jsonPath("$[1].id").value(id2.toString()));
  }
}
