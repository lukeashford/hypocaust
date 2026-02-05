package com.example.hypocaust.web;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.domain.Todo;
import java.util.List;
import java.util.UUID;

public record TaskExecutionSnapshot(
    UUID taskExecutionId,
    TaskExecutionStatus status,
    List<Artifact> artifacts,
    List<Todo> todos,
    UUID lastEventId
) {

}
