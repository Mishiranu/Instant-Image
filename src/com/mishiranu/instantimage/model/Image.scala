package com.mishiranu.instantimage.model

import android.os.Parcel
import android.os.Parcelable

case class Image(imageUriString: String, thumbnailUriString: String, text: String) extends Parcelable {
  override def describeContents: Int = 0

  override def writeToParcel(dest: Parcel, flags: Int): Unit = {
    dest.writeString(imageUriString)
    dest.writeString(thumbnailUriString)
    dest.writeString(text)
  }
}

object Image {
  val CREATOR = new Parcelable.Creator[Image] {
    override def createFromParcel(source: Parcel): Image = {
      val imageUriString = source.readString()
      val thumbnailUriString = source.readString()
      val text = source.readString()
      Image(imageUriString, thumbnailUriString, text)
    }

    override def newArray(size: Int): Array[Image] = new Array(size)
  }
}
