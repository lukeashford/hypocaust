package com.example.hypocaust.tool;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.db.ArtifactChunkEntity;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.repo.ArtifactChunkRepository;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.EmbeddingService;
import com.example.hypocaust.service.VersionManagementService;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchProjectTool {

  private static final int HISTORY_PAGE_SIZE = 50;
  private static final int CHUNK_RESULTS = 5;
  private static final String HISTORY_SCOPE = "history";
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final TaskExecutionRepository taskExecutionRepository;
  private final ArtifactChunkRepository artifactChunkRepository;
  private final EmbeddingService embeddingService;
  private final VersionManagementService versionManagementService;

  @Tool(name = "search_project", description = """
      Search project content and history by natural language query.

      scope = "history": returns the task execution log (50 at a time) — what was done, what \
                         changed, when. Use offset to paginate further back. \
                         Use to find historical artifact versions, execution names, or past attempts.
      scope = <artifact name>: semantic search within that artifact's content fields only. \
                         Use when you know which artifact to look in but need specific text \
                         (e.g. a dialogue on page 30 of a script).
      scope = omitted: semantic search across all artifact content in the project. \
                         Use when you don't know which artifact contains the relevant concept.

      Returns verbatim matching text chunks with artifact name and field path.""")
  public String search(
      @ToolParam(description = "Natural language search query") String query,
      @ToolParam(description = "Artifact name, 'history', or omit for all",
          required = false) String scope,
      @ToolParam(description = "For history scope: skip this many entries (default 0)",
          required = false) Integer offset) {

    if (HISTORY_SCOPE.equalsIgnoreCase(scope)) {
      return searchHistory(offset);
    }
    return searchChunks(query, scope);
  }

  private String searchHistory(Integer offset) {
    UUID projectId = TaskExecutionContextHolder.getProjectId();
    List<TaskExecutionEntity> all = taskExecutionRepository
        .findByProjectIdOrderByCreatedAtDesc(projectId);
    int total = all.size();
    int skip = offset != null ? offset : 0;

    List<TaskExecutionEntity> page = all.stream()
        .skip(skip)
        .limit(HISTORY_PAGE_SIZE)
        .toList();

    if (page.isEmpty()) {
      return "No task execution history found.";
    }

    var sb = new StringBuilder();
    for (TaskExecutionEntity te : page) {
      String date = te.getCreatedAt().atOffset(ZoneOffset.UTC).format(DATE_FMT);
      sb.append(String.format("[%s] %s \"%s\" — %s%n",
          te.getStatus(),
          date,
          te.getName() != null ? te.getName() : "unnamed",
          te.getCommitMessage() != null ? te.getCommitMessage() : "no summary"));

      TaskExecutionDelta delta = te.getDelta();
      if (delta != null && delta.hasChanges()) {
        String changes = Stream.of(
                delta.added().stream().map(c -> c.name() + " (added)"),
                delta.edited().stream().map(c -> c.name() + " (edited)"),
                delta.deleted().stream().map(n -> n + " (deleted)"))
            .flatMap(s -> s)
            .collect(Collectors.joining(", "));
        sb.append("  changed: ").append(changes).append('\n');
      }
    }

    int shown = Math.min(skip + page.size(), total);
    sb.append(String.format("%nShowing entries %d–%d of %d", skip + 1, shown, total));

    return sb.toString();
  }

  private String searchChunks(String query, String scope) {
    UUID projectId = TaskExecutionContextHolder.getProjectId();
    float[] queryEmbedding = embeddingService.generateEmbedding(query);

    List<ArtifactChunkEntity> results;
    if (scope != null && !scope.isBlank()) {
      UUID predecessorId = TaskExecutionContextHolder.getPredecessorId();
      Artifact artifact = versionManagementService
          .getMaterializedArtifactAt(scope, predecessorId)
          .or(() -> TaskExecutionContextHolder.getContext().getArtifacts().get(scope))
          .orElse(null);
      if (artifact == null || artifact.id() == null) {
        return "Artifact not found: " + scope;
      }
      results = artifactChunkRepository.findByArtifactSimilarity(
          artifact.id(), queryEmbedding, PageRequest.of(0, CHUNK_RESULTS));
    } else {
      results = artifactChunkRepository.findByProjectSimilarity(
          projectId, queryEmbedding, PageRequest.of(0, CHUNK_RESULTS));
    }

    if (results.isEmpty()) {
      return "No matching content found.";
    }

    var sb = new StringBuilder();
    for (ArtifactChunkEntity chunk : results) {
      sb.append(String.format("[%s / %s, chunk %d]%n%s%n%n",
          resolveArtifactName(chunk.getArtifactId()),
          chunk.getFieldPath(),
          chunk.getChunkIndex(),
          chunk.getText()));
    }

    return sb.toString().stripTrailing();
  }

  private String resolveArtifactName(UUID artifactId) {
    return TaskExecutionContextHolder.getContext().getArtifacts().getAllWithChanges().stream()
        .filter(a -> artifactId.equals(a.id()))
        .map(Artifact::name)
        .findFirst()
        .orElse(artifactId.toString());
  }
}
