package com.mishiranu.instantimage.ui

import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.{Gravity, View, ViewGroup}
import android.widget.{BaseAdapter, FrameLayout, ImageView, LinearLayout, TextView}

import com.mishiranu.instantimage.model.Image
import com.mishiranu.instantimage.util.Preferences
import com.mishiranu.instantimage.widget.SquareImageView
import com.squareup.picasso.Picasso

import scala.collection.mutable

class GridAdapter extends BaseAdapter {
  private val images = new mutable.ArrayBuffer[Image]

  def setImages(images: Seq[Image]): Unit = {
    this.images.clear()
    this.images ++= images
    notifyDataSetChanged()
  }

  override def getItem(position: Int): Image = images(position)
  override def getItemId(position: Int): Long = 0L
  override def getCount: Int = images.size

  case class ViewHolder(imageView: SquareImageView, dimensionsView: TextView, titleView: TextView)

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val (view, viewHolder) = {
      if (convertView == null) {
        val density = parent.getResources.getDisplayMetrics.density
        val frameLayout = new FrameLayout(parent.getContext)
        frameLayout.setBackgroundColor(0xff7f7f7f)
        val typedArray = parent.getContext.obtainStyledAttributes(Array(android.R.attr.selectableItemBackground))
        frameLayout.setForeground(typedArray.getDrawable(0))
        typedArray.recycle()

        val imageView = new SquareImageView(parent.getContext)
        frameLayout.addView(imageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val linearLayout = new LinearLayout(parent.getContext)
        linearLayout.setOrientation(LinearLayout.VERTICAL)
        linearLayout.setBackgroundColor(0xcc222222)
        val padding = (8 * density + 0.5f).toInt
        linearLayout.setPadding(padding, padding, padding, padding)
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        val dimensionsView = new TextView(parent.getContext)
        dimensionsView.setSingleLine(true)
        dimensionsView.setEllipsize(TextUtils.TruncateAt.END)
        dimensionsView.setTextColor(0xddffffff)
        dimensionsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12)
        dimensionsView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        linearLayout.addView(dimensionsView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val titleView = new TextView(parent.getContext)
        titleView.setSingleLine(true)
        titleView.setEllipsize(TextUtils.TruncateAt.END)
        titleView.setTextColor(0xffffffff)
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12)
        linearLayout.addView(titleView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val viewHolder = ViewHolder(imageView, dimensionsView, titleView)
        frameLayout.setTag(viewHolder)
        (frameLayout, viewHolder)
      } else {
        (convertView, convertView.getTag.asInstanceOf[ViewHolder])
      }
    }

    val image = getItem(position)
    val scaleType = if (Preferences.cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
    viewHolder.imageView.setScaleType(scaleType)
    Picasso.`with`(parent.getContext).load(image.thumbnailUriString).into(viewHolder.imageView)
    if (image.width > 0 && image.height > 0) {
      viewHolder.dimensionsView.setVisibility(View.VISIBLE)
      viewHolder.dimensionsView.setText(image.width + " × " + image.height)
    } else {
      viewHolder.dimensionsView.setVisibility(View.GONE)
    }
    viewHolder.titleView.setText(image.text)
    view
  }
}
