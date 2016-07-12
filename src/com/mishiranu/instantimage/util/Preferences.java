package com.mishiranu.instantimage.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences
{
	private static volatile SharedPreferences sPreferences; 
	
	private static SharedPreferences getPreferences(Context context)
	{
		if (sPreferences == null)
		{
			synchronized (Preferences.class)
			{
				if (sPreferences == null) sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			}
		}
		return sPreferences;
	}
	
	private static final String KEY_CONTENT_ID = "content_id";
	
	public static int nextContentId(Context context)
	{
		synchronized (Preferences.class)
		{
			int id = getPreferences(context).getInt(KEY_CONTENT_ID, 0) + 1;
			sPreferences.edit().putInt(KEY_CONTENT_ID, id).commit();
			return id;
		}
	}
	
	private static final String KEY_CROP_THUMBNAILS = "crop_thumbnails";
	
	public static boolean isCropThumbnails(Context context)
	{
		return getPreferences(context).getBoolean(KEY_CROP_THUMBNAILS, true);
	}
	
	public static void setCropThumbnails(Context context, boolean cropThumbnails)
	{
		getPreferences(context).edit().putBoolean(KEY_CROP_THUMBNAILS, cropThumbnails).commit();
	}
}