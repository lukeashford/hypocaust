package com.example.the_machine.service;

import com.example.the_machine.config.AppConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class ArtifactPanelService {

  private final AppConfig appConfig;

  public String loadRunStatusArtifact(UUID threadId) throws IOException {
    var res = new ClassPathResource("artifact/RunStatusArtifact.tsx");
    final String tsx_template = new String(res.getInputStream().readAllBytes(),
        StandardCharsets.UTF_8);

    final String tsx = tsx_template.replace(
        "__HOST_URL",
        appConfig.getHostUrl()
    ).replace(
        "__THREAD_ID__",
        threadId.toString()
    );
    final String identifier = "run-status-artifact";
    final String mimeType = "application/vnd.react";
    final String title = "Run Status";

    return String.format(
        ":::artifact{identifier=\"%s\" type=\"%s\" title=\"%s\"}\n```\n%s\n```\n:::\n",
        identifier, mimeType, title, tsx
    );
  }

  public String loadRunMonitor(UUID threadId) {
    final String identifier = "run-monitor";
    final String mimeType = "application/vnd.react";
    final String title = "Run Monitor";

    return String.format(
        ":::artifact{identifier=\"%s\" type=\"%s\" title=\"%s\"}\n```\n%s\n```\n:::\n",
        identifier, mimeType, title, "Loading..."
    );
  }
}