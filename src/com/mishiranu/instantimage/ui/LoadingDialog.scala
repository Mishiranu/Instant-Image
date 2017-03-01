package com.mishiranu.instantimage.ui

import android.app.{DialogFragment, ProgressDialog}
import android.os.Bundle

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.async.{LoadImageTask, LoadQueryTask}
import com.mishiranu.instantimage.model.Image

import rx.lang.scala.Subscription

class LoadingDialog extends DialogFragment with RxFragment {
  import LoadingDialog._

  private var callback: Callback = _

  private def this(data: Either[String, Iterable[String]], callback: LoadingDialog.Callback) = {
    this
    val args = new Bundle
    data match {
      case Left(query) =>
        args.putString(EXTRA_QUERY, query)
      case Right(imageUriStrings) =>
        import scala.collection.JavaConverters._
        args.putStringArrayList(EXTRA_IMAGE_URI_STRINGS, new java.util.ArrayList(imageUriStrings.toList.asJava))
    }
    setArguments(args)
    setCallback(callback)
  }

  def this(query: String, callback: LoadingDialog.Callback) = {
    this(Left(query), callback)
  }

  def this(imageUriStrings: Iterable[String], callback: LoadingDialog.Callback) = {
    this(Right(imageUriStrings), callback)
  }

  def setCallback(callback: Callback): Unit = {
    this.callback = callback
  }

  override def onDetach(): Unit = {
    super.onDetach()
    callback = null
  }

  override def onCreateDialog(savedInstanceState: Bundle): ProgressDialog = {
    val dialog = new ProgressDialog(getActivity)
    dialog.setMessage(getString(R.string.text_loading))
    dialog.setCanceledOnTouchOutside(false)
    dialog
  }

  override protected def createRxSubscription: Subscription = {
    val query = getArguments.getString(EXTRA_QUERY)
    val imageUriStrings = getArguments.getStringArrayList(EXTRA_IMAGE_URI_STRINGS)
    if (query != null) {
      LoadQueryTask(query, 0).subscribe(onQueryLoad _)
    } else if (imageUriStrings != null) {
      import scala.collection.JavaConverters._
      LoadImageTask(getActivity, imageUriStrings.asScala.toList).subscribe(onImagesLoad _)
    } else {
      throw new RuntimeException
    }
  }

  private def onQueryLoad(result: LoadQueryTask.Result): Unit = handleRxResult {
    dismiss()
    if (result.errorMessageId != 0) {
      callback.onQueryLoadingError(result.errorMessageId)
    } else if (result.images.isEmpty) {
      callback.onQueryLoadingError(R.string.error_not_found)
    } else {
      callback.onQueryLoadingSuccess(result.images)
    }
  }

  private def onImagesLoad(results: List[LoadImageTask.Result]): Unit = handleRxResult {
    dismiss()
    val successResults = results.filter(_.errorMessageId == 0)
    if (successResults.nonEmpty) {
      callback.onImageLoadingSuccess(successResults.map(r => ImageLoadingSuccessResult(r.contentId, r.mimeType)))
    } else {
      callback.onImageLoadingError(results.map(_.errorMessageId).find(_ != 0).get)
    }
  }
}

object LoadingDialog {
  val TAG: String = classOf[LoadingDialog].getName

  private val EXTRA_QUERY = "query"
  private val EXTRA_IMAGE_URI_STRINGS = "imageUriStrings"

  trait Callback {
    def onQueryLoadingSuccess(images: List[Image]): Unit
    def onQueryLoadingError(errorMessageId: Int): Unit

    def onImageLoadingSuccess(results: List[ImageLoadingSuccessResult]): Unit
    def onImageLoadingError(errorMessageId: Int): Unit
  }

  case class ImageLoadingSuccessResult(contentId: Int, mimeType: String)
}
