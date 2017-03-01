package com.mishiranu.instantimage.graphics

import android.graphics.{Canvas, Color, Paint, Path, PathMeasure}
import android.view.animation.DecelerateInterpolator

class CheckSelectorDrawable extends SelectorDrawable {
  import CheckSelectorDrawable._

  private val paint = new Paint(Paint.ANTI_ALIAS_FLAG)
  paint.setStyle(Paint.Style.STROKE)
  paint.setStrokeCap(Paint.Cap.SQUARE)
  paint.setStrokeJoin(Paint.Join.MITER)
  paint.setColor(Color.WHITE)

  private val path = new Path
  private val pathMeasure = new PathMeasure

  private var start: Long = _
  private var selected = false

  override def setSelected(selected: Boolean, animate: Boolean): Unit = {
    if (this.selected != selected) {
      if (animate) {
        start = System.currentTimeMillis()
      } else {
        start = 0L
      }
      this.selected = selected
      invalidateSelf()
    }
  }

  override def draw(canvas: Canvas): Unit = {
    val dt = System.currentTimeMillis - start
    val rawValue = if (start == 0L) 1f else if (dt < 0) 0f else Math.min(dt.toFloat / DURATION, 1f)
    val value = DECELERATE_INTERPOLATOR.getInterpolation(rawValue)
    val bounds = getBounds
    canvas.save()
    canvas.translate(bounds.left, bounds.top)
    canvas.drawColor(Color.argb(((if (selected) value else 1f - value) * 0x80).toInt, 0, 0, 0))
    val width = bounds.width()
    val height = bounds.height()
    val size = if (width > height) {
      canvas.translate((width - height) / 2, 0)
      height
    } else if (height > width) {
      canvas.translate(0, (height - width) / 2)
      width
    } else {
      width
    }
    val strokeSize = 0.03f
    paint.setStrokeWidth(strokeSize * size)

    path.moveTo(0.39f * size, 0.5f * size)
    path.rLineTo(0.08f * size, 0.08f * size)
    path.rLineTo(0.14f * size, -0.14f * size)
    pathMeasure.setPath(path, false)
    path.rewind()

    /* scope */ {
      val length = pathMeasure.getLength
      if (selected) {
        pathMeasure.getSegment(0f, value * length, path, true)
      } else {
        pathMeasure.getSegment(value * length, length, path, true)
      }
    }

    canvas.drawPath(path, paint)
    path.rewind()

    val append = if (selected)  (1f - value) * 90f else value * -90f - 180f
    paint.setStrokeWidth(strokeSize * size)
    path.arcTo(0.3f * size, 0.3f * size, 0.7f * size, 0.7f * size, 270f + append, -180f, true)
    path.arcTo(0.3f * size, 0.3f * size, 0.7f * size, 0.7f * size, 90f + append, -180f, false)
    pathMeasure.setPath(path, false)
    path.rewind()

    /* scope */ {
      val length = pathMeasure.getLength
      if (selected) {
        pathMeasure.getSegment(0f, value * length, path, true)
      } else {
        pathMeasure.getSegment(value * length, length, path, true)
      }
    }

    canvas.drawPath(path, paint)
    path.rewind()

    canvas.restore()
    if (value < 1f) {
      invalidateSelf()
    }
  }
}

object CheckSelectorDrawable {
  private val DURATION = 200
  private val DECELERATE_INTERPOLATOR = new DecelerateInterpolator
}
