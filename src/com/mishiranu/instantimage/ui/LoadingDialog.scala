package com.mishiranu.instantimage.ui

import android.app.{DialogFragment, ProgressDialog}
import android.content.DialogInterface
import android.os.Bundle

import com.mishiranu.instantimage.R
import com.mishiranu.instantimage.async.{LoadImageTask, LoadQueryTask}
import com.mishiranu.instantimage.model.Image

import rx.lang.scala.Subscription

class LoadingDialog extends DialogFragment {
  private var callback: LoadingDialog.Callback = _
  private var subscription: Subscription = _

  def this(query: String, imageUriString: String, callback: LoadingDialog.Callback) = {
    this
    val args = new Bundle
    args.putString(LoadingDialog.EXTRA_QUERY, query)
    args.putString(LoadingDialog.EXTRA_IMAGE_URI_STRING, imageUriString)
    setArguments(args)
    setCallback(callback)
  }

  def setCallback(callback: LoadingDialog.Callback): Unit = {
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

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    val query = getArguments.getString(LoadingDialog.EXTRA_QUERY)
    val imageUriString = getArguments.getString(LoadingDialog.EXTRA_IMAGE_URI_STRING)
    subscription = if (query != null) {
      LoadQueryTask(query).subscribe(onQueryLoad _)
    } else if (imageUriString != null) {
      LoadImageTask(getActivity, imageUriString).subscribe(onImageLoad _)
    } else {
      throw new RuntimeException
    }
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    unsubscribe()
  }

  override def onCancel(dialog: DialogInterface): Unit = {
    super.onCancel(dialog)
    unsubscribe()
  }

  private def unsubscribe(): Unit = {
    if (subscription != null) {
      subscription.unsubscribe()
      subscription = null
    }
  }

  private var resumed = false
  private var queuedResult: () => Unit = _

  override def onResume(): Unit = {
    super.onResume()
    resumed = true
    if (queuedResult != null) {
      val result = queuedResult
      queuedResult = null
      handleResult(result())
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    resumed = false
  }

  private def handleResult(result: => Unit): Unit = {
    if (resumed) {
      dismiss()
      result
    } else {
      queuedResult = () => result
    }
  }

  private def onQueryLoad(result: LoadQueryTask.Result): Unit = {
    unsubscribe()
    handleResult {
      if (result.errorMessageId != 0) {
        callback.onQueryLoadingError(result.errorMessageId)
      } else if (result.images.isEmpty) {
        callback.onQueryLoadingError(R.string.error_not_found)
      } else {
        callback.onQueryLoadingSuccess(result.images)
      }
    }
  }

  private def onImageLoad(result: LoadImageTask.Result): Unit = {
    unsubscribe()
    handleResult {
      if (result.errorMessageId != 0) {
        callback.onImageLoadingError(result.errorMessageId)
      } else {
        callback.onImageLoadingSuccess(result.contentId, result.mimeType)
      }
    }
  }
}

object LoadingDialog {
  val TAG: String = LoadingDialog.getClass.getName

  private val EXTRA_QUERY = "query"
  private val EXTRA_IMAGE_URI_STRING = "imageUriString"

  trait Callback {
    def onQueryLoadingSuccess(images: List[Image]): Unit
    def onQueryLoadingError(errorMessageId: Int): Unit

    def onImageLoadingSuccess(contentId: Int, mimeType: String): Unit
    def onImageLoadingError(errorMessageId: Int): Unit
  }
}
