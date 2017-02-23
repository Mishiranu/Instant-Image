package com.mishiranu.instantimage.model

import android.os.Parcel
import android.os.Parcelable

case class Image(imageUriString: String, thumbnailUriString: String, text: String,
  width: Int, height: Int) extends Parcelable {
  override def describeContents: Int = 0

  override def writeToParcel(dest: Parcel, flags: Int): Unit = {
    dest.writeString(imageUriString)
    dest.writeString(thumbnailUriString)
    dest.writeString(text)
    dest.writeInt(width)
    dest.writeInt(height)
  }
}

object Image {
  val CREATOR = new Parcelable.Creator[Image] {
    override def createFromParcel(source: Parcel): Image = {
      val imageUriString = source.readString()
      val thumbnailUriString = source.readString()
      val text = source.readString()
      val width = source.readInt()
      val height = source.readInt()
      Image(imageUriString, thumbnailUriString, text, width, height)
    }

    override def newArray(size: Int): Array[Image] = new Array(size)
  }
}
