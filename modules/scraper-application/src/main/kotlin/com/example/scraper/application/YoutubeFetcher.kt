package com.example.scraper.application

import com.example.scraper.domain.Result
import com.example.scraper.domain.ScraperError
import com.fasterxml.jackson.databind.ObjectMapper
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Implementation of YoutubeMetadataFetcher that uses NewPipe extractor library.
 */
@Component
@Primary
class YoutubeFetcher(
  private val objectMapper: ObjectMapper,
  private val metricsService: ScraperMetricsService
) : YoutubeMetadataFetcher {

  private val logger = LoggerFactory.getLogger(YoutubeFetcher::class.java)

  // Initialize NewPipe
  init {
    NewPipe.init(DownloaderImpl())
  }

  private val youtubeService = ServiceList.YouTube

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
   * Fetches metadata from the specified YouTube video URL using NewPipe extractor.
   *
   * @param url The YouTube video URL to fetch metadata from.
   * @return Result containing the YouTube metadata if successful, or a failure with an error.
   */
  override fun fetch(url: String): Result<YoutubeMetadataFetcher.YoutubeMetadata> {
    return metricsService.measureTime(fetchTimer) {
      try {
        logger.info("Fetching YouTube metadata from URL: $url")

        // Validate URL
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) {
          fetchErrorCounter.increment()
          return@measureTime Result.Failure(
            ScraperError.UnexpectedError(
              "Invalid YouTube URL: $url",
              null
            )
          )
        }

        // Extract video ID from URL
        val videoId = extractVideoId(url)
        if (videoId.isNullOrBlank()) {
          fetchErrorCounter.increment()
          return@measureTime Result.Failure(
            ScraperError.UnexpectedError(
              "Invalid YouTube URL: $url",
              null
            )
          )
        }

        // Get video info from NewPipe
        logger.info("Getting video info from NewPipe for video ID: $videoId")

        // Create a LinkHandler for the video
        val linkHandler = youtubeService.streamLHFactory.fromId(videoId)

        // Extract video info
        val extractor = youtubeService.getStreamExtractor(linkHandler)
        extractor.fetchPage()

        val title = extractor.name
        val description = extractor.description.content

        // Convert video info to JSON
        val jsonOutput = objectMapper.writeValueAsString(
          mapOf(
            "id" to videoId,
            "name" to title,
            "uploaderName" to extractor.uploaderName,
            "uploadDate" to extractor.uploadDate,
            "description" to description,
            "viewCount" to extractor.viewCount,
            "likeCount" to extractor.likeCount,
            "duration" to extractor.length
          )
        )

        // Get subtitles if available
        val subtitles = mutableMapOf<String, String>()
        val subtitleList = extractor.subtitlesDefault
        if (subtitleList.isNotEmpty()) {
          for (subtitle in subtitleList) {
            try {
              // Get subtitle content
              val subtitleUrl = subtitle.url
              if (subtitleUrl != null) {
                try {
                  // For simplicity, we'll just log that we found a subtitle
                  // In a real implementation, you would fetch the subtitle content
                  logger.info("Found subtitle for language: ${subtitle.languageTag}")
                  subtitles[subtitle.languageTag] = "Subtitle content would be fetched here"
                  subtitleCounter.increment()
                } catch (e: Exception) {
                  logger.warn("Failed to process subtitle for language: ${subtitle.languageTag}", e)
                }
              }
            } catch (e: Exception) {
              logger.warn("Failed to get subtitle content for language: ${subtitle.languageTag}", e)
            }
          }
        }

        logger.info("Successfully fetched YouTube metadata from URL: $url, title: $title")

        fetchSuccessCounter.increment()
        Result.Success(
          YoutubeMetadataFetcher.YoutubeMetadata(
            title = title,
            description = description,
            json = jsonOutput,
            subtitles = subtitles.ifEmpty { null }
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

  /**
   * Extracts the video ID from a YouTube URL.
   *
   * @param url The YouTube URL.
   * @return The video ID, or null if the URL is invalid.
   */
  private fun extractVideoId(url: String): String? {
    val regex =
      Regex("""(?:youtube\.com(?:[^\n\s]+\S+|(?:v|e(?:mbed)?)|\S*?[?&]v=)|youtu\.be)([a-zA-Z0-9_-]{11})""")
    val matchResult = regex.find(url)
    return matchResult?.groupValues?.getOrNull(1)
  }
}
