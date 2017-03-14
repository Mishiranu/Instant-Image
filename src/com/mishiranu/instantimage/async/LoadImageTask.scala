package com.mishiranu.instantimage.async

import android.content.Context

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.util.{FileManager, Preferences}
import com.mishiranu.instantimage.util.ScalaHelpers._

import com.squareup.okhttp.Response

import rx.android.schedulers.AndroidSchedulers
import rx.lang.scala.Observable
import rx.schedulers.Schedulers

import java.io.{FileOutputStream, IOException}

import javax.net.ssl.SSLException

class LoadImageTask(context: Context, uriString: String) extends RxHttp {
  import LoadImageTask._

  private class InvalidResponseException extends Exception

  private def request: Observable[Response] = {
    request(uriString).header("Referer", uriString.replaceAll("(https?://.*?/).*", "$1")).execute()
  }

  private def getImage(response: Response): Array[Byte] = {
    if (response.code != 200) {
      throw new InvalidResponseException
    }
    response.body.bytes
  }

  private def writeToFile(response: Array[Byte]): Result = {
    val contentId = Preferences.nextContentId()
    val fileItem = FileManager.obtainFile(context, uriString, contentId)
    var outputStream: FileOutputStream = null
    try {
      outputStream = new FileOutputStream(fileItem.file)
      outputStream.write(response)
    } finally {
      if (outputStream != null) {
        outputStream.close()
      }
    }
    Result(contentId, fileItem.mimeType, 0)
  }

  private def handleError(t: Throwable): Result = {
    Result(0, null, t match {
      case _: InvalidResponseException => R.string.error_invalid_response
      case _: SSLException => R.string.error_ssl
      case _: IOException => R.string.error_connection
      case _ => R.string.error_unknown
    })
  }

  def load: Observable[Result] = {
    request.subscribeOn(Schedulers.io)
      .map(getImage)
      .map(writeToFile)
      .onErrorReturn(handleError)
      .observeOn(AndroidSchedulers.mainThread)
  }
}

object LoadImageTask {
  case class Result(contentId: Int, mimeType: String, errorMessageId: Int)

  def apply(context: Context, uriStrings: Iterable[String]): Observable[List[Result]] = {
    Observable.combineLatest(uriStrings.map(s => new LoadImageTask(context.getApplicationContext, s).load))(_.toList)
  }
}
