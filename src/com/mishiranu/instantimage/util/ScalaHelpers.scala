package com.mishiranu.instantimage.util

import android.view.View

import rx.lang.scala.Scheduler

import scala.language.implicitConversions

object ScalaHelpers {
  implicit def functionToRunnable(function: => Unit): Runnable = new Runnable {
    override def run(): Unit = function
  }

  object Runnable {
    def apply(function: => Unit): Runnable = functionToRunnable(function)
  }

  implicit def functionToOnClickListener(function: android.view.View => Unit):
    android.view.View.OnClickListener = new android.view.View.OnClickListener {
    override def onClick(v: View): Unit = function(v)
  }

  private class JavaSchedulerWrapper(val asJavaScheduler: rx.Scheduler) extends Scheduler

  implicit def rxScheduler(scheduler: rx.Scheduler): Scheduler = {
    new JavaSchedulerWrapper(scheduler)
  }

  implicit def tupleToPair[A, B](tuple: (A, B)): android.util.Pair[A, B] = {
    (android.util.Pair.create[A, B] _).tupled(tuple)
  }

  def sdk[T](min: Int = 0, max: Int = Int.MaxValue)(function: PartialFunction[Boolean, T]): Option[T] = {
    val value = android.os.Build.VERSION.SDK_INT >= min && android.os.Build.VERSION.SDK_INT <= max
    if (function.isDefinedAt(value)) Some(function(value)) else None
  }
}
