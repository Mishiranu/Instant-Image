package com.mishiranu.instantimage.async;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLException;

import android.content.Context;

import com.mishiranu.instantimage.R;
import com.mishiranu.instantimage.util.FileManager;
import com.mishiranu.instantimage.util.Preferences;

public class LoadImageTask extends AsyncManager.SimpleTask<Void, Void, Void>
{
	private final Context mContext;
	private final String mUriString;
	
	private int mErrorMessageId;
	private int mContentId;
	private String mMimeType;
	
	public LoadImageTask(Context context, String uriString)
	{
		mContext = context.getApplicationContext();
		mUriString = uriString;
	}
	
	@Override
	protected Void doInBackground(Void... params)
	{
		FileManager.cleanup(mContext);
		HttpURLConnection connection = null;
		try
		{
			connection = (HttpURLConnection) new URL(mUriString).openConnection();
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				mErrorMessageId = R.string.error_invalid_response;
				return null;
			}
			if (isCancelled()) return null;
			InputStream input = connection.getInputStream();
			int contentId = Preferences.nextContentId(mContext);
			FileManager.FileItem fileItem = FileManager.obtainFile(mContext, mUriString, contentId);
			FileOutputStream output = new FileOutputStream(fileItem.file);
			int count;
			byte[] buffer = new byte[16 * 1024];
			while ((count = input.read(buffer)) >= 0 && !isCancelled()) output.write(buffer, 0, count);
			output.close();
			mContentId = contentId;
			mMimeType = fileItem.getMimeType();
		}
		catch (SSLException e)
		{
			mErrorMessageId = R.string.error_ssl;
		}
		catch (IOException e)
		{
			mErrorMessageId = R.string.error_connection;
		}
		finally
		{
			if (connection != null) connection.disconnect();
		}
		return null;
	}
	
	@Override
	protected void onStoreResult(AsyncManager.Holder holder, Void result)
	{
		holder.storeResult(mErrorMessageId, mContentId, mMimeType);
	}
}