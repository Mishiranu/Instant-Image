package com.mishiranu.instantimage.util;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.webkit.MimeTypeMap;

public class FileManager
{
	public static void cleanup(Context context)
	{
		File[] files = context.getFilesDir().listFiles();
		if (files != null)
		{
			long time = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
			for (File file : files)
			{
				if (file.lastModified() < time) file.delete();
			}
		}
	}
	
	public static class FileItem
	{
		public final int contentId;
		public final String displayName;
		public final File file;
		
		public FileItem(int contentId, String displayName, File file)
		{
			this.contentId = contentId;
			this.displayName = displayName;
			this.file = file;
		}
		
		public String getMimeType()
		{
			String extension = null;
			int index = displayName.lastIndexOf('.');
			if (index >= 0) extension = displayName.substring(index + 1);
			if (extension == null || extension.isEmpty()) extension = "jpeg";
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mimeType == null) mimeType = "image/jpeg";
			return mimeType;
		}
	}
	
	public static String obtainSimpleFileName(String originalUriString)
	{
		String extension = null;
		if (originalUriString != null)
		{
			int index = originalUriString.lastIndexOf('.');
			if (index >= 0 && originalUriString.indexOf('/', index) == -1)
			{
				String extensionTemp = originalUriString.substring(index + 1);
				if (extensionTemp.length() <= 5) extension = extensionTemp;
			}
		}
		if (extension == null || extension.isEmpty()) extension = "jpeg";
		return System.currentTimeMillis() + "." + extension;
	}
	
	public static FileItem obtainFile(Context context, String originalUriString, int contentId)
	{
		File filesDir = context.getFilesDir();
		filesDir.mkdirs();
		String displayName = contentId + "-" + obtainSimpleFileName(originalUriString);
		return new FileItem(contentId, displayName, new File(filesDir, displayName));
	}
	
	public static ArrayList<FileItem> listFiles(Context context)
	{
		ArrayList<FileItem> fileItems = new ArrayList<>();
		File[] files = context.getFilesDir().listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				String[] splitted = file.getName().split("-");
				if (splitted.length == 2)
				{
					fileItems.add(new FileItem(Integer.parseInt(splitted[0]), splitted[1], file));
				}
			}
		}
		return fileItems;
	}
	
	public static FileItem findFile(Context context, int contentId)
	{
		ArrayList<FileItem> fileItems = listFiles(context);
		for (FileItem fileItem : fileItems)
		{
			if (fileItem.contentId == contentId) return fileItem;
		}
		return null;
	}
}