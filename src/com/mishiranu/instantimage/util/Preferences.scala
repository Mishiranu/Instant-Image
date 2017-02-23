package com.mishiranu.instantimage.util

import android.content.SharedPreferences
import android.preference.PreferenceManager

import com.mishiranu.instantimage.ui.MainApplication

object Preferences {
  lazy val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication())

  private val KEY_CONTENT_ID = "content_id"

  def nextContentId(): Int = {
    Preferences.synchronized {
      val id = preferences.getInt(KEY_CONTENT_ID, 0) + 1
      preferences.edit().putInt(KEY_CONTENT_ID, id).commit()
      id
    }
  }

  private val KEY_CROP_THUMBNAILS = "crop_thumbnails"

  def cropThumbnails: Boolean = {
    preferences.getBoolean(KEY_CROP_THUMBNAILS, true)
  }

  def setCropThumbnails(cropThumbnails: Boolean): Unit = {
    preferences.edit().putBoolean(KEY_CROP_THUMBNAILS, cropThumbnails).commit()
  }
}
