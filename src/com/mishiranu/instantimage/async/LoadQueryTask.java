package com.mishiranu.instantimage.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.SSLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.mishiranu.instantimage.ImageItem;
import com.mishiranu.instantimage.R;

public class LoadQueryTask extends AsyncManager.SimpleTask<Void, Void, Void>
{
	private final String mQuery;
	
	private int mErrorMessageId;
	private ArrayList<ImageItem> mImageItems;
	
	public LoadQueryTask(String query)
	{
		mQuery = query;
	}
	
	@Override
	protected Void doInBackground(Void... params)
	{
		Uri uri = Uri.parse("https://www.google.com/search?tbm=isch").buildUpon()
				.appendQueryParameter("q", mQuery).build();
		HttpURLConnection connection = null;
		try
		{
			connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);
			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:47.0) "
					+ "Gecko/20100101 Firefox/47.0");
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				mErrorMessageId = R.string.error_invalid_response;
				return null;
			}
			if (isCancelled()) return null;
			InputStream input = connection.getInputStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int count;
			byte[] buffer = new byte[16 * 1024];
			while ((count = input.read(buffer)) >= 0 && !isCancelled()) output.write(buffer, 0, count);
			if (isCancelled()) return null;
			String data = new String(output.toByteArray());
			int index = data.indexOf("<body");
			if (index >= 0)
			{
				while (true)
				{
					int start = data.indexOf('{', index + 1);
					if (start == -1) break;
					int end = data.indexOf('}', start);
					if (end == -1) break;
					index = end;
					String jsonString = data.substring(start, end + 1);
					try
					{
						JSONObject jsonObject = new JSONObject(jsonString);
						String imageUriString = jsonObject.getString("ou");
						String thumbnailUriString = jsonObject.getString("tu");
						String text = jsonObject.getString("pt");
						if (mImageItems == null) mImageItems = new ArrayList<>();
						mImageItems.add(new ImageItem(imageUriString, thumbnailUriString, text));
					}
					catch (JSONException e)
					{
						
					}
				}
			}
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
		holder.storeResult(mErrorMessageId, mImageItems);
	}
}