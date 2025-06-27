package com.example.scraper.application

import com.example.scraper.domain.Result

/**
 * Interface for fetching metadata from YouTube videos.
 */
interface YoutubeMetadataFetcher {

  /**
   * Data class representing the result of a YouTube metadata fetch operation.
   */
  data class YoutubeMetadata(
    val title: String,
    val description: String?,
    val json: String,
    val subtitles: Map<String, String>? // Language code to subtitle content
  )

  /**
   * Fetches metadata from the specified YouTube video URL.
   *
   * @param url The YouTube video URL to fetch metadata from.
   * @return Result containing the YouTube metadata if successful, or a failure with an error.
   */
  fun fetch(url: String): Result<YoutubeMetadata>
}