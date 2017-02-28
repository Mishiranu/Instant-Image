package com.mishiranu.instantimage.async

import android.net.Uri

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.model.Image
import com.mishiranu.instantimage.util.ScalaHelpers._

import org.json.{JSONArray, JSONException, JSONObject}

import rx.android.schedulers.AndroidSchedulers
import rx.lang.scala.Observable
import rx.schedulers.Schedulers

import scalaj.http.HttpResponse

import java.io.IOException

import javax.net.ssl.SSLException

class LoadQueryTask(query: String) extends Http {
  import LoadQueryTask._

  private class InvalidResponseException extends Exception

  private def request(page: Int): Observable[HttpResponse[String]] = {
    Observable[HttpResponse[String]] { e =>
      val uriString = Uri.parse("https://www.google.com/search?tbm=isch&asearch=ichunk").buildUpon
        .appendQueryParameter("q", query).appendQueryParameter("start", (100 * page).toString).build.toString
      e.onNext(http(uriString).asString)
      e.onCompleted()
    }
  }

  private def getString(response: HttpResponse[String]): String = {
    if (response.code != 200) {
      throw new InvalidResponseException
    }
    response.body
  }

  private def getImages(response: String): Result = {
    def getJsonArrayData[T](jsonArray: JSONArray, key: String, getter: JSONArray => Int => T): Option[T] = {
      (0 until jsonArray.length)
        .filter(_ % 2 == 0)
        .filter(jsonArray.getString(_) == key)
        .map(i => getter(jsonArray)(i + 1))
        .headOption
    }

    val dom = try {
      val jsonArray = new JSONArray(response)
      getJsonArrayData(jsonArray, "rg_s", _.getJSONArray)
        .flatMap(getJsonArrayData(_, "dom", _.getString))
        .getOrElse(throw new InvalidResponseException)
    } catch {
      case _: JSONException => throw new InvalidResponseException
    }

    def getImageString(data: String, from: Int): List[String] = {
      val start = data.indexOf('{', from + 1)
      val end = data.indexOf('}', start)
      if (end > start && start > from) {
        getImageString(data, end) ::: List(data.substring(start, end + 1))
      } else {
        List()
      }
    }

    val index = dom.indexOf("<div")
    val list = if (index >= 0) getImageString(dom, index) else List()

    Result(list.map { jsonString =>
      try {
        val jsonObject = new JSONObject(jsonString)
        val imageUriString = jsonObject.getString("ou")
        val thumbnailUriString = jsonObject.getString("tu")
        val text = jsonObject.getString("pt")
        val width = jsonObject.optInt("ow")
        val height = jsonObject.optInt("oh")
        Image(imageUriString, thumbnailUriString, text, width, height)
      } catch {
        case _: JSONException => null
      }
    }.filter(_ != null), 0)
  }

  private def handleError(t: Throwable): Result = {
    Result(null, t match {
      case _: InvalidResponseException => R.string.error_invalid_response
      case _: SSLException => R.string.error_ssl
      case _: IOException => R.string.error_connection
      case _ => R.string.error_unknown
    })
  }

  def load(page: Int): Observable[Result] = {
    request(page).subscribeOn(Schedulers.io)
      .map(getString)
      .map(getImages)
      .onErrorReturn(handleError)
      .observeOn(AndroidSchedulers.mainThread)
  }
}

object LoadQueryTask {
  case class Result(images: List[Image], errorMessageId: Int)

  def apply(query: String, page: Int): Observable[Result] = {
    new LoadQueryTask(query).load(page)
  }
}
