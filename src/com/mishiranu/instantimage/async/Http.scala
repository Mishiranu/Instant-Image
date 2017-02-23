package com.mishiranu.instantimage.async

import scalaj.http.{BaseHttp, HttpOptions, HttpRequest}

trait Http {
  private val userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:47.0) Gecko/20100101 Firefox/47.0"

  private object Http extends BaseHttp(userAgent = userAgent)

  def http(url: String): HttpRequest = {
    Http(url).timeout(15000, 15000).option(HttpOptions.followRedirects(true))
  }
}
