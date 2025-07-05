package com.example.scraper.application

import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Implementation of YoutubeMetadataFetcher that shells out to youtube-dl.
 */
@Component
class YoutubeDlFetcher(
  private val objectMapper: ObjectMapper,
  private val metricsService: ScraperMetricsService
) : YoutubeMetadataFetcher {

  private val logger = LoggerFactory.getLogger(YoutubeDlFetcher::class.java)

  private val fetchTimer = metricsService.createTimer(
    "scraper.youtube.fetch.time",
    "Time taken to fetch YouTube metadata"
  )

  private val fetchSuccessCounter = metricsService.createCounter(
    "scraper.youtube.fetch.success.count",
    "Number of successful YouTube metadata fetches"
  )

  private val fetchErrorCounter = metricsService.createCounter(
    "scraper.youtube.fetch.error.count",
    "Number of failed YouTube metadata fetches"
  )

  private val subtitleCounter = metricsService.createCounter(
    "scraper.youtube.subtitles.count",
    "Number of subtitles fetched from YouTube videos"
  )

  /**
   * Fetches metadata from the specified YouTube video URL using youtube-dl.
   *
   * @param url The YouTube video URL to fetch metadata from.
   * @return Result containing the YouTube metadata if successful, or a failure with an error.
   */
  override fun fetch(url: String): Result<YoutubeMetadataFetcher.YoutubeMetadata> {
    return metricsService.measureTime(fetchTimer) {
      try {
        logger.info("Fetching YouTube metadata from URL: $url")

        // Create a temporary directory for subtitles
        val tempDir = Files.createTempDirectory("youtube-dl-")

        // Build the youtube-dl command
        val command = listOf(
          "youtube-dl",
          "--dump-json",
          "--write-sub",
          "--sub-lang", "en,de,fr,es,it",
          "--sub-format", "vtt",
          "--output", "${tempDir.toAbsolutePath()}/%(id)s.%(ext)s",
          url
        )

        // Execute the command
        val process = ProcessBuilder(command)
          .redirectErrorStream(true)
          .start()

        // Read the JSON output
        val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
        val jsonOutput = reader.readText()

        // Wait for the process to complete
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
          process.destroyForcibly()
          fetchErrorCounter.increment()
          return@measureTime Result.Failure(
            ScraperError.UnexpectedError(
              "Timeout while executing youtube-dl for URL: $url",
              null
            )
          )
        }

        if (process.exitValue() != 0) {
          fetchErrorCounter.increment()
          return@measureTime Result.Failure(
            ScraperError.UnexpectedError(
              "youtube-dl exited with non-zero status for URL: $url. Output: $jsonOutput",
              null
            )
          )
        }

        // Parse the JSON output
        val jsonNode = objectMapper.readTree(jsonOutput)
        val title = jsonNode.get("title")?.asText() ?: "No Title"
        val description = jsonNode.get("description")?.asText()
        val videoId = jsonNode.get("id")?.asText()

        // Read subtitles if available
        val subtitles = mutableMapOf<String, String>()
        if (videoId != null) {
          val subtitleFiles = File(tempDir.toFile().absolutePath).listFiles { file ->
            file.name.startsWith(videoId) && file.name.endsWith(".vtt")
          }

          subtitleFiles?.forEach { file ->
            // Extract language code from filename (format: videoId.LANG.vtt)
            val langCode =
              file.name.substringAfterLast(videoId).substringAfter(".").substringBefore(".")
            if (langCode.isNotBlank()) {
              val subtitleContent = file.readText(StandardCharsets.UTF_8)
              subtitles[langCode] = subtitleContent
              subtitleCounter.increment()
            }
          }
        }

        logger.info("Successfully fetched YouTube metadata from URL: $url, title: $title")

        // Clean up temporary directory
        tempDir.toFile().deleteRecursively()

        fetchSuccessCounter.increment()
        Result.Success(
          YoutubeMetadataFetcher.YoutubeMetadata(
            title = title,
            description = description,
            json = jsonOutput,
            subtitles = if (subtitles.isEmpty()) null else subtitles
          )
        )
      } catch (e: Exception) {
        logger.error("Error fetching YouTube metadata from URL: $url", e)
        fetchErrorCounter.increment()
        Result.Failure(
          ScraperError.UnexpectedError(
            "Failed to fetch YouTube metadata from URL: $url: ${e.message}",
            e
          )
        )
      }
    } ?: run {
      logger.error("Timed block returned null")
      fetchErrorCounter.increment()
      Result.Failure(ScraperError.UnexpectedError("measureScrapeTime yielded null"))
    }
  }
}
