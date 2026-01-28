package com.example.hypocaust.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.service.ArtifactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ArtifactController.class)
class ArtifactControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArtifactService artifactService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testGetArtifactMetadata() throws Exception {
    UUID artifactId = UUID.randomUUID();
    ArtifactDto dto = new ArtifactDto(
        "test_image",
        Kind.IMAGE,
        "A test image description",
        "/artifacts/" + artifactId + "/content",
        false,
        Status.CREATED
    );

    when(artifactService.getArtifactDto(artifactId)).thenReturn(dto);

    mockMvc.perform(get("/artifacts/{artifactId}", artifactId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("test_image"))
        .andExpect(jsonPath("$.description").value("A test image description"))
        .andExpect(jsonPath("$.kind").value("IMAGE"));
  }

  @Test
  void testGetArtifactContentJson() throws Exception {
    UUID artifactId = UUID.randomUUID();
    ObjectNode contentNode = objectMapper.createObjectNode().put("hello", "world");

    ArtifactEntity entity = ArtifactEntity.builder()
        .kind(Kind.STRUCTURED_JSON)
        .status(Status.CREATED)
        .content(contentNode)
        .build();
    ReflectionTestUtils.setField(entity, "id", artifactId);

    when(artifactService.getArtifact(artifactId)).thenReturn(entity);

    MvcResult mvcResult = mockMvc.perform(get("/artifacts/{artifactId}/content", artifactId))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.hello").value("world"));
  }

  @Test
  void testGetArtifactContentFile() throws Exception {
    UUID artifactId = UUID.randomUUID();
    byte[] content = "fake-image-bytes".getBytes();

    ArtifactEntity entity = ArtifactEntity.builder()
        .kind(Kind.IMAGE)
        .status(Status.CREATED)
        .name("test_image")
        .storageKey("some-key")
        .build();
    ReflectionTestUtils.setField(entity, "id", artifactId);

    when(artifactService.getArtifact(artifactId)).thenReturn(entity);
    when(artifactService.downloadArtifact(artifactId)).thenReturn(
        new ByteArrayInputStream(content));

    MvcResult mvcResult = mockMvc.perform(get("/artifacts/{artifactId}/content", artifactId))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"test_image\""))
        .andExpect(content().bytes(content));
  }

  @Test
  void testGetArtifactContentNotReady() throws Exception {
    UUID artifactId = UUID.randomUUID();

    ArtifactEntity entity = ArtifactEntity.builder()
        .kind(Kind.IMAGE)
        .status(Status.SCHEDULED)
        .build();
    ReflectionTestUtils.setField(entity, "id", artifactId);

    when(artifactService.getArtifact(artifactId)).thenReturn(entity);

    mockMvc.perform(get("/artifacts/{artifactId}/content", artifactId))
        .andExpect(status().isAccepted()); // ArtifactNotReadyException -> 202
  }

  @Test
  void testGetArtifactWithInvalidId() throws Exception {
    mockMvc.perform(get("/artifacts/undefined"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Invalid value 'undefined' for parameter 'artifactId'"));
  }
}
