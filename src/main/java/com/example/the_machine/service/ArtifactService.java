package com.example.the_machine.service;

import com.example.the_machine.common.Routes;
import com.example.the_machine.config.AppConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class ArtifactService {

  private final AppConfig appConfig;

  public String loadRunStatusArtifact(UUID threadId) throws IOException {
    var res = new ClassPathResource("artifact/RunStatusArtifact.tsx");
    final String tsx_template = new String(res.getInputStream().readAllBytes(),
        StandardCharsets.UTF_8);

    final String tsx = tsx_template.replace(
        "__STREAM_URL__",
        appConfig.getHostUrl() + Routes.THREAD_EVENTS.replace("{id}", threadId.toString())
    );
    final String identifier = "run-status-artifact";
    final String mimeType = "application/vnd.react";
    final String title = "Run Status";

    return String.format(
        ":::artifact{identifier=\"%s\" type=\"%s\" title=\"%s\"}\n```\n%s\n```\n:::\n",
        identifier, mimeType, title, tsx
    );
  }
}