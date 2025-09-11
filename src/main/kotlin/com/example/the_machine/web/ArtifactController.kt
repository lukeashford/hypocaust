package com.example.the_machine.web

import com.example.the_machine.common.Routes
import com.example.the_machine.repo.ArtifactRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Controller for handling artifact file downloads.
 */
@RestController
class ArtifactController(
  private val artifactRepository: ArtifactRepository
) {

  private val log = LoggerFactory.getLogger(ArtifactController::class.java)

  /**
   * Downloads an artifact file by ID.
   *
   * @param id the artifact ID
   * @return the file resource with proper headers
   */
  @GetMapping(Routes.ARTIFACTS_BY_ID)
  fun downloadArtifact(@PathVariable id: UUID): ResponseEntity<Resource> {
    log.info("Downloading artifact: {}", id)

    return try {
      // Look up artifact
      val artifact = artifactRepository.findById(id).orElse(null)
      if (artifact == null) {
        log.warn("Artifact not found: {}", id)
        return ResponseEntity.notFound().build()
      }

      // Check if storage key is present
      val storageKey = artifact.storageKey
      if (storageKey.isNullOrBlank()) {
        log.warn("Artifact {} has no storage key", id)
        return ResponseEntity.notFound().build()
      }

      // Get file path
      val filePath = Paths.get(storageKey)

      // Check if file exists
      if (!Files.exists(filePath)) {
        log.warn("File not found for artifact {}: {}", id, filePath)
        return ResponseEntity.notFound().build()
      }

      // Create resource
      val resource = FileSystemResource(filePath)

      // Determine content type
      val contentType = determineContentType(artifact.mime, filePath)

      // Build response headers
      val headers = HttpHeaders().apply {
        setContentType(contentType)
        contentLength = Files.size(filePath)

        // Set filename for download
        artifact.title?.let { title ->
          val filename = sanitizeFilename(title) + getFileExtension(filePath)
          contentDisposition = ContentDisposition.attachment()
            .filename(filename)
            .build()
        }
      }

      log.info("Streaming artifact file: {} ({})", id, filePath)
      ResponseEntity.ok()
        .headers(headers)
        .body(resource)

    } catch (e: IOException) {
      log.error("Error reading artifact file for ID: {}", id, e)
      ResponseEntity.internalServerError().build()
    } catch (e: Exception) {
      log.error("Unexpected error downloading artifact: {}", id, e)
      ResponseEntity.internalServerError().build()
    }
  }

  /**
   * Determines the content type from mime field or file extension.
   */
  private fun determineContentType(mime: String?, filePath: Path): MediaType {
    // Use stored mime type if available
    if (!mime.isNullOrBlank()) {
      try {
        return MediaType.parseMediaType(mime)
      } catch (e: Exception) {
        log.warn("Invalid mime type '{}': {}", mime, e.message)
      }
    }

    // Fallback to file extension
    val fileName = filePath.fileName.toString().lowercase()
    return when {
      fileName.endsWith(".png") -> MediaType.IMAGE_PNG
      fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> MediaType.IMAGE_JPEG
      fileName.endsWith(".pdf") -> MediaType.APPLICATION_PDF
      fileName.endsWith(".json") -> MediaType.APPLICATION_JSON
      fileName.endsWith(".txt") -> MediaType.TEXT_PLAIN
      else -> MediaType.APPLICATION_OCTET_STREAM
    }
  }

  /**
   * Sanitizes filename for safe download.
   */
  private fun sanitizeFilename(filename: String?): String =
    filename?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "artifact"

  /**
   * Gets file extension from path.
   */
  private fun getFileExtension(filePath: Path): String {
    val fileName = filePath.fileName.toString()
    val lastDot = fileName.lastIndexOf('.')
    return if (lastDot > 0 && lastDot < fileName.length - 1) {
      fileName.substring(lastDot)
    } else {
      ""
    }
  }
}