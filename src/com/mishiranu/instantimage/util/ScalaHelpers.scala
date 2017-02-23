package com.mishiranu.instantimage.util

import rx.lang.scala.Scheduler

object ScalaHelpers {
  implicit def functionToRunnable(function: () => Unit): Runnable = new Runnable {
    override def run(): Unit = function()
  }

  private class JavaSchedulerWrapper(val asJavaScheduler: rx.Scheduler) extends Scheduler

  implicit def rxScheduler(scheduler: rx.Scheduler): Scheduler = {
    new JavaSchedulerWrapper(scheduler)
  }
}
