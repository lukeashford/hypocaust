package com.example.hypocaust.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${app.host-url:http://localhost:8080}")
  private String hostUrl;

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Hypocaust API")
            .version("0.1.0")
            .description("""
                Task orchestration API for AI-powered content generation.

                ## Core Workflow
                1. **Create a project** via `POST /projects` to get a `projectId`.
                2. **Submit a task** via `POST /tasks` with the `projectId`. \
                   Returns a `taskExecutionId` and a `firstEventId`.
                3. **Subscribe to SSE** at `GET /task-executions/{id}/events` \
                   (Accept: text/event-stream) to receive real-time updates. \
                   Pass `Last-Event-ID: {firstEventId}` to skip the initial started event, \
                   or omit it for a full replay.
                4. **Poll state** via `GET /task-executions/{id}/state` if SSE is unavailable or on reconnect.

                ## SSE Events
                Events are delivered as Server-Sent Events with the following structure:
                - `id` — UUID, monotonically increasing within an execution
                - `event` — the event type (e.g. `artifact.added`, `todo.list.updated`)
                - `data` — JSON payload

                On reconnect, pass the last received event id as `Last-Event-ID` header \
                to replay missed events.
                """))
        .servers(List.of(new Server().url(hostUrl)));
  }
}
