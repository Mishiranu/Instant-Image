package com.mishiranu.instantimage.async

import com.squareup.okhttp.{OkHttpClient, Request, Response}

import rx.lang.scala.Observable

import scala.language.implicitConversions

import java.util.concurrent.TimeUnit

trait RxHttp {
  private val client = new OkHttpClient()

  List(client.setConnectTimeout _, client.setReadTimeout _, client.setWriteTimeout _)
    .foreach(_(15000, TimeUnit.MILLISECONDS))

  private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:51.0) Gecko/20100101 Firefox/51.0"

  def request(url: String): Request.Builder = {
    new Request.Builder().url(url).header("User-Agent", userAgent)
  }

  class ExecutableBuilder(builder: Request.Builder) {
    def execute(): Observable[Response] = Observable { e =>
      try {
        e.onNext(client.newCall(builder.build()).execute())
        e.onCompleted()
      } catch {
        case t: Throwable => e.onError(t)
      }
    }
  }

  implicit def executable(request: Request.Builder): ExecutableBuilder = new ExecutableBuilder(request)
}
