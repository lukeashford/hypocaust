package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response DTO for user-facing error messages. This provides a consistent,
 * user-friendly error response structure that hides technical details from end users.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardErrorResponseDto(
    String message,
    String timestamp
) {

}