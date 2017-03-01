package com.mishiranu.instantimage.graphics

import android.content.Context
import android.graphics.{Canvas, Color, Paint}

class BorderSelectorDrawable(context: Context) extends SelectorDrawable {
  import BorderSelectorDrawable._

  private val paint = new Paint(Paint.ANTI_ALIAS_FLAG)
  paint.setColor(Color.BLACK)
  private val density = context.getResources.getDisplayMetrics.density

  private var selected = false

  override def setSelected(selected: Boolean, animated: Boolean): Unit = {
    this.selected = selected
    invalidateSelf()
  }

  override def draw(canvas: Canvas): Unit = {
    if (selected) {
      canvas.drawColor(0x7f000000)
      val bounds = getBounds
      val thickness = (THICKNESS_DP * density).toInt
      canvas.drawRect(bounds.top, bounds.left, bounds.right, bounds.top + thickness, paint)
      canvas.drawRect(bounds.bottom - thickness, bounds.left, bounds.right, bounds.bottom, paint)
      canvas.drawRect(bounds.top, bounds.left, bounds.left + thickness, bounds.bottom, paint)
      canvas.drawRect(bounds.top, bounds.right - thickness, bounds.right, bounds.bottom, paint)
    }
  }
}

object BorderSelectorDrawable {
  private val THICKNESS_DP = 2
}
