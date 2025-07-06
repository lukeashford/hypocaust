package com.example.scraper.application

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Implementation of NewPipe's Downloader interface using OkHttp.
 */
class DownloaderImpl() : Downloader() {

  private val logger = LoggerFactory.getLogger(DownloaderImpl::class.java)
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  override fun execute(request: Request): Response {
    logger.debug("Executing request: ${request.url()}")

    val okHttpRequestBuilder = okhttp3.Request.Builder()
      .url(request.url())

    // Add headers
    for (headerName in request.headers().keys) {
      val headerValues = request.headers()[headerName]
      if (headerValues != null) {
        for (headerValue in headerValues) {
          okHttpRequestBuilder.addHeader(headerName, headerValue)
        }
      }
    }

    // Set request method and body if needed
    when (request.httpMethod()) {
      "GET" -> okHttpRequestBuilder.get()
      "POST" -> {
        val requestBody = request.dataToSend()?.toRequestBody(null)
        okHttpRequestBuilder.post(requestBody ?: "".toRequestBody(null))
      }

      "HEAD" -> okHttpRequestBuilder.head()
      else -> throw UnsupportedOperationException("Unsupported request method: ${request.httpMethod()}")
    }

    val okHttpRequest = okHttpRequestBuilder.build()

    return try {
      val okHttpResponse = client.newCall(okHttpRequest).execute()

      // Create response headers map
      val responseHeaders = HashMap<String, List<String>>()
      for (headerName in okHttpResponse.headers.names()) {
        responseHeaders[headerName] = okHttpResponse.headers.values(headerName)
      }

      // Get response body
      val responseBodyBytes = okHttpResponse.body?.bytes() ?: ByteArray(0)
      val responseBodyString = String(responseBodyBytes)

      // Create response
      Response(
        okHttpResponse.code,
        okHttpResponse.message,
        responseHeaders,
        responseBodyString,
        okHttpResponse.request.url.toString()
      )
    } catch (e: Exception) {
      logger.error("Error executing request: ${request.url()}", e)
      throw e
    }
  }
}
