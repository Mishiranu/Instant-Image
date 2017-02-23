package com.mishiranu.instantimage.ui

import android.app.Application
import android.content.Context

class MainApplication extends Application {
  MainApplication.self = this
}

object MainApplication {
  private var self: Context = _
  def apply(): Context = self
}
