package com.mishiranu.instantimage.ui

import java.io.FileNotFoundException

import android.content.{ContentProvider, ContentValues, UriMatcher}
import android.database.{Cursor, MatrixCursor}
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.{BaseColumns, OpenableColumns}
import com.mishiranu.instantimage.util.FileManager

class ImageProvider extends ContentProvider {
  override def onCreate(): Boolean = true

  override def getType(uri: Uri): String = {
    ImageProvider.URI_MATCHER.`match`(uri) match {
      case ImageProvider.URI_IMAGES =>
        "vnd.android.cursor.dir/" + ImageProvider.AUTHORITY + "." + ImageProvider.PATH_IMAGES
      case ImageProvider.URI_IMAGES_ID =>
        FileManager.findFile(getContext, uri.getLastPathSegment.toInt)
          .map(_.mimeType).getOrElse("application/octet-stream")
      case _ =>
        throw new IllegalArgumentException("Unknown URI: " + uri)
    }
  }

  override def openFile(uri: Uri, mode: String): ParcelFileDescriptor = {
    ImageProvider.URI_MATCHER.`match`(uri) match {
      case ImageProvider.URI_IMAGES_ID =>
        if (!"r".equals(mode)) {
          throw new FileNotFoundException
        }
        FileManager.findFile(getContext, Integer.parseInt(uri.getLastPathSegment))
          .map(f => ParcelFileDescriptor.open(f.file, ParcelFileDescriptor.MODE_READ_ONLY))
          .getOrElse(throw new FileNotFoundException)
      case _ =>
        throw new FileNotFoundException
    }
  }

  override def query(uri: Uri, projection: Array[String], selection: String,
    selectionArgs: Array[String], sortOrder: String): Cursor = {
    val matchResult = ImageProvider.URI_MATCHER.`match`(uri)
    val multiple = matchResult == ImageProvider.URI_IMAGES
    if (matchResult == ImageProvider.URI_IMAGES || matchResult == ImageProvider.URI_IMAGES_ID) {
      val workProjection = if (projection != null) projection else ImageProvider.PROJECTION

      workProjection
        .filterNot(_ == BaseColumns._ID)
        .filterNot(_ == OpenableColumns.DISPLAY_NAME)
        .filterNot(_ == OpenableColumns.SIZE)
        .headOption.map(c => throw new SQLiteException("No such column: " + c))

      val cursor = new MatrixCursor(workProjection)
      val contentId = if (multiple) -1 else uri.getLastPathSegment.toInt

      FileManager.listFiles(getContext)
        .filter(f => contentId == -1 || f.contentId == contentId)
        .map{ fileItem =>
        Map(BaseColumns._ID -> fileItem.contentId, OpenableColumns.DISPLAY_NAME -> fileItem.displayName,
          OpenableColumns.SIZE -> fileItem.file.length)
      }.foreach(map => cursor.addRow(workProjection.map(c => map(c).asInstanceOf[AnyRef])))
      cursor
    } else {
      throw new IllegalArgumentException("Unknown URI: " + uri)
    }
  }

  override def insert(uri: Uri, values: ContentValues): Uri = {
    throw new SQLiteException("Unsupported operation")
  }

  override def update(uri: Uri, values: ContentValues, selection: String, selectionArgs: Array[String]): Int = {
    throw new SQLiteException("Unsupported operation")
  }

  override def delete(uri: Uri, selection: String, selectionArgs: Array[String]): Int = {
    throw new SQLiteException("Unsupported operation")
  }
}

object ImageProvider {
  private val AUTHORITY = "com.mishiranu.providers.instantimage"
  private val PATH_IMAGES = "images"

  private val URI_IMAGES = 1
  private val URI_IMAGES_ID = 2

  private val URI_MATCHER = {
    val matcher = new UriMatcher(UriMatcher.NO_MATCH)
    matcher.addURI(AUTHORITY, PATH_IMAGES, URI_IMAGES)
    matcher.addURI(AUTHORITY, PATH_IMAGES + "/#", URI_IMAGES_ID)
    matcher
  }

  def buildUri(contentId: Int): Uri = {
    Uri.parse("content://" + AUTHORITY + "/" + PATH_IMAGES + "/" + contentId)
  }

  private def PROJECTION = Array(BaseColumns._ID, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
}
