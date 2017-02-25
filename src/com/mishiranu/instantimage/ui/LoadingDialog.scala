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

  def this(query: String, imageUriString: String, callback: LoadingDialog.Callback) = {
    this
    val args = new Bundle
    args.putString(LoadingDialog.EXTRA_QUERY, query)
    args.putString(LoadingDialog.EXTRA_IMAGE_URI_STRING, imageUriString)
    setArguments(args)
    setCallback(callback)
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
    val imageUriString = getArguments.getString(EXTRA_IMAGE_URI_STRING)
    if (query != null) {
      LoadQueryTask(query).subscribe(onQueryLoad _)
    } else if (imageUriString != null) {
      LoadImageTask(getActivity, imageUriString).subscribe(onImageLoad _)
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

  private def onImageLoad(result: LoadImageTask.Result): Unit = handleRxResult {
    dismiss()
    if (result.errorMessageId != 0) {
      callback.onImageLoadingError(result.errorMessageId)
    } else {
      callback.onImageLoadingSuccess(result.contentId, result.mimeType)
    }
  }
}

object LoadingDialog {
  val TAG: String = classOf[LoadingDialog].getName

  private val EXTRA_QUERY = "query"
  private val EXTRA_IMAGE_URI_STRING = "imageUriString"

  trait Callback {
    def onQueryLoadingSuccess(images: List[Image]): Unit
    def onQueryLoadingError(errorMessageId: Int): Unit

    def onImageLoadingSuccess(contentId: Int, mimeType: String): Unit
    def onImageLoadingError(errorMessageId: Int): Unit
  }
}
