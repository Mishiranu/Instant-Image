package com.mishiranu.instantimage.graphics

import android.graphics.{ColorFilter, PixelFormat}
import android.graphics.drawable.Drawable

trait SelectorDrawable extends Drawable {
  def setSelected(selected: Boolean, animated: Boolean): Unit

  override def getOpacity: Int = PixelFormat.TRANSLUCENT
  override def setAlpha(alpha: Int): Unit = ()
  override def setColorFilter(cf: ColorFilter): Unit = ()
}
