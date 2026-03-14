package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Receipt for a staged upload.")
public record UploadReceiptDto(
    @Schema(description = "Unique identifier for this upload, used for cancellation")
    UUID dataPackageId,

    @Schema(description = "Staging batch identifier. Send this on subsequent uploads to group "
        + "them, and on task submission to associate uploads with the task")
    UUID batchId
) {

}
