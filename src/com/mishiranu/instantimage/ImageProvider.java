package com.mishiranu.instantimage;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.OpenableColumns;

import com.mishiranu.instantimage.util.FileManager;
import com.mishiranu.instantimage.util.FileManager.FileItem;

public class ImageProvider extends ContentProvider
{
	private static final String AUTHORITY = "com.mishiranu.providers.instantimage";
	private static final String PATH_IMAGES = "images";
	
	private static final int URI_IMAGES = 1;
	private static final int URI_IMAGES_ID = 2;
	
	private static final UriMatcher URI_MATCHER;
	
	static
	{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, PATH_IMAGES, URI_IMAGES);
		URI_MATCHER.addURI(AUTHORITY, PATH_IMAGES + "/#", URI_IMAGES_ID);
	}
	
	public static Uri build(int contentId)
	{
		return Uri.parse("content://" + AUTHORITY + "/" + PATH_IMAGES + "/" + contentId);
	}
	
	@Override
	public boolean onCreate()
	{
		return true;
	}
	
	@Override
	public String getType(Uri uri)
	{
		switch (URI_MATCHER.match(uri))
		{
			case URI_IMAGES: return "vnd.android.cursor.dir/" + AUTHORITY + "." + PATH_IMAGES;
			case URI_IMAGES_ID:
			{
				FileManager.FileItem fileItem = FileManager.findFile(getContext(),
						Integer.parseInt(uri.getLastPathSegment()));
				if (fileItem != null) return fileItem.getMimeType();
				return "application/octet-stream";
			}
			default: throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
	
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
	{
		switch (URI_MATCHER.match(uri))
		{
			case URI_IMAGES_ID:
			{
				if (!"r".equals(mode)) throw new FileNotFoundException();
				FileManager.FileItem fileItem = FileManager.findFile(getContext(),
						Integer.parseInt(uri.getLastPathSegment()));
				if (fileItem != null)
				{
					return ParcelFileDescriptor.open(fileItem.file, ParcelFileDescriptor.MODE_READ_ONLY);
				}
			}
			default: throw new FileNotFoundException();
		}
	}
	
	private static final String[] PROJECTION = {BaseColumns._ID, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		boolean multiple = false;
		switch (URI_MATCHER.match(uri))
		{
			case URI_IMAGES:
			{
				multiple = true;
			}
			case URI_IMAGES_ID:
			{
				if (projection == null) projection = PROJECTION;
				for (String column : projection)
				{
					switch (column)
					{
						case BaseColumns._ID: break;
						case OpenableColumns.DISPLAY_NAME: break;
						case OpenableColumns.SIZE: break;
						default: throw new SQLiteException("No such column: " + column);
					}
				}
				MatrixCursor cursor = new MatrixCursor(projection);
				int contentId = multiple ? -1 : Integer.parseInt(uri.getLastPathSegment());
				ArrayList<FileManager.FileItem> fileItems = FileManager.listFiles(getContext());
				Object[] values = new Object[projection.length];
				for (FileItem fileItem : fileItems)
				{
					if (contentId == -1 || fileItem.contentId == contentId)
					{
						for (int i = 0; i < projection.length; i++)
						{
							switch (projection[i])
							{
								case BaseColumns._ID:
								{
									values[i] = fileItem.contentId;
									break;
								}
								case OpenableColumns.DISPLAY_NAME:
								{
									values[i] = fileItem.displayName;
									break;
								}
								case OpenableColumns.SIZE:
								{
									values[i] = fileItem.file.length();
									break;
								}
							}
						}
						cursor.addRow(values);
					}
				}
				return cursor;
			}
			default: throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		throw new SQLiteException("Unsupported operation");
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		throw new SQLiteException("Unsupported operation");
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		throw new SQLiteException("Unsupported operation");
	}
}