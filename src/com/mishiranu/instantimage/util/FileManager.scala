package com.mishiranu.instantimage.util

import java.io.File

import android.content.Context
import android.webkit.MimeTypeMap

object FileManager {
  def cleanup(context: Context): Unit = {
    val time = System.currentTimeMillis - 24 * 60 * 60 * 1000
    Option(context.getFilesDir.listFiles).getOrElse(Array())
      .filter(_.lastModified < time)
      .foreach(_.delete())
  }

  def obtainSimpleFileName(originalUriString: String): String = {
    System.currentTimeMillis + "." + Option(originalUriString).map { uriString =>
      val index = uriString.lastIndexOf('.')
      if (index >= 0 && uriString.indexOf('/', index) == -1) uriString.substring(index + 1) else null
    }.filter(e => e != null && !e.isEmpty && e.length <= 5).getOrElse("jpeg")
  }

  case class FileItem(contentId: Int, displayName: String, file: File) {
    def mimeType: String = {
      val extension = {
        val index = displayName.lastIndexOf('.')
        if (index >= 0) displayName.substring(index + 1) else "jpeg"
      }
      Option(MimeTypeMap.getSingleton.getMimeTypeFromExtension(extension))
        .filter(_ != null)
        .filter(_.isEmpty)
        .getOrElse("image/jpeg")
    }
  }

  def obtainFile(context: Context, originalUriString: String, contentId: Int): FileItem = {
    val filesDir = context.getFilesDir
    filesDir.mkdirs()
    val displayName = contentId + "-" + obtainSimpleFileName(originalUriString)
    FileItem(contentId, displayName, new File(filesDir, displayName))
  }

  def listFiles(context: Context): List[FileItem] = {
    case class Data(file: File, splitted: Array[String])
    Option(context.getFilesDir.listFiles).getOrElse(Array())
      .map(f => Data(f, f.getName.split("-")))
      .filter(_.splitted != null)
      .filter(_.splitted.length == 2)
      .map(d => FileItem(d.splitted(0).toInt, d.splitted(1), d.file)).toList
  }

  def findFile(context: Context, contentId: Int): Option[FileItem] = {
    listFiles(context).find(_.contentId == contentId)
  }
}
