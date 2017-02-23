package com.mishiranu.instantimage.widget

import android.content.Context
import android.widget.ImageView

class SquareImageView(context: Context) extends ImageView(context) {
  override protected def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(getMeasuredWidth, getMeasuredWidth)
  }
}
