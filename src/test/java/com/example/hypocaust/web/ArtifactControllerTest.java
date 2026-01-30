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
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
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
    Artifact dto = new Artifact(
        "test_image",
        ArtifactKind.IMAGE,
        "/artifacts/" + artifactId + "/content",
        null,
        "A test image title",
        "A test image description",
        ArtifactStatus.CREATED,
        null
    );

    when(artifactService.getArtifactDomain(artifactId)).thenReturn(dto);

    mockMvc.perform(get("/artifacts/{artifactId}", artifactId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("test_image"))
        .andExpect(jsonPath("$.description").value("A test image description"))
        .andExpect(jsonPath("$.kind").value("IMAGE"));
  }

  @Test
  void testGetArtifactContentJson() throws Exception {
    UUID artifactId = UUID.randomUUID();
    ObjectNode contentNode = objectMapper.createObjectNode().put("hello", "world");

    ArtifactEntity entity = ArtifactEntity.builder()
        .kind(ArtifactKind.STRUCTURED_JSON)
        .status(ArtifactStatus.CREATED)
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
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .fileName("test_image")
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
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.GESTATING)
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
