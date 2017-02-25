package com.mishiranu.instantimage.ui

import android.app.Fragment
import android.os.Bundle

import rx.lang.scala.Subscription

trait RxFragment extends Fragment {
  private var subscription: Subscription = _

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    subscription = createRxSubscription
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
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
      handleRxResult(result())
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    resumed = false
  }

  protected def handleRxResult(result: => Unit): Unit = {
    unsubscribe()
    if (resumed) {
      result
    } else {
      queuedResult = () => result
    }
  }

  protected def createRxSubscription: Subscription
}
