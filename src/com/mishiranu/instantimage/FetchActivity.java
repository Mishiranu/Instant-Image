package com.mishiranu.instantimage;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.Toast;

import com.mishiranu.instantimage.util.FileManager;
import com.mishiranu.instantimage.util.Preferences;

public class FetchActivity extends Activity implements ViewTreeObserver.OnGlobalLayoutListener,
		SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener, LoadingDialog.Callback,
		AdapterView.OnItemClickListener
{
	private static final String EXTRA_IMAGE_ITEMS = "imageItems";
	
	private ArrayList<ImageItem> mImageItems;
	private final GridAdapter mGridAdapter = new GridAdapter();

	private SearchView mSearchView;
	private GridView mGridView;
	
	private static final int GRID_SPACING_DP = 4;
	
	private boolean mKeepSearchFocus = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mSearchView = new SearchView(getActionBar().getThemedContext());
		mSearchView.setOnQueryTextListener(this);
		mKeepSearchFocus = savedInstanceState == null;
		mGridView = new GridView(this);
		mGridView.setId(android.R.id.list);
		mGridView.setOnItemClickListener(this);
		mGridView.setAdapter(mGridAdapter);
		float density = getResources().getDisplayMetrics().density;
		int spacing = (int) (GRID_SPACING_DP * density + 0.5f);
		mGridView.setHorizontalSpacing(spacing);
		mGridView.setVerticalSpacing(spacing);
		mGridView.setPadding(spacing, spacing, spacing, spacing);
		mGridView.setScrollBarStyle(GridView.SCROLLBARS_OUTSIDE_OVERLAY);
		mGridView.setClipToPadding(false);
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(this);
		registerForContextMenu(mGridView);
		addContentView(mGridView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		LoadingDialog loadingDialog = (LoadingDialog) getFragmentManager().findFragmentByTag(LoadingDialog.TAG);
		if (loadingDialog != null) loadingDialog.setCallback(this);
		if (savedInstanceState != null)
		{
			ArrayList<ImageItem> imageItems = savedInstanceState.getParcelableArrayList(EXTRA_IMAGE_ITEMS);
			if (imageItems != null)
			{
				mImageItems = imageItems;
				mGridAdapter.setImageItems(imageItems);
			}
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(EXTRA_IMAGE_ITEMS, mImageItems);
	}
	
	private final Runnable mLayoutRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			int position = mGridView.getFirstVisiblePosition();
			float density = getResources().getDisplayMetrics().density;
			int sizeDp = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 200 : 150;
			int size = (int) (sizeDp * density + 0.5f);
			int spacing = mGridView.getHorizontalSpacing();
			int numColumns = (mLastGridViewWidth - spacing) / (size + spacing);
			mGridView.setNumColumns(numColumns);
			mGridView.setSelection(position);
		}
	};
	
	private int mLastGridViewWidth = -1;
	
	@Override
	public void onGlobalLayout()
	{
		int width = mGridView.getWidth();
		if (mLastGridViewWidth != width)
		{
			mLastGridViewWidth = width;
			mGridView.removeCallbacks(mLayoutRunnable);
			mGridView.post(mLayoutRunnable);
		}
	}
	
	@Override
	public boolean onQueryTextChange(String newText)
	{
		return false;
	}
	
	@Override
	public boolean onQueryTextSubmit(String query)
	{
		mSearchView.clearFocus();
		if (query.length() > 0) new LoadingDialog(query, null, this).show(getFragmentManager(), LoadingDialog.TAG);
		return false;
	}
	
	@Override
	public boolean onMenuItemActionExpand(MenuItem item)
	{
		return true;
	}
	
	@Override
	public boolean onMenuItemActionCollapse(MenuItem item)
	{
		finish();
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, 1, 0, "").setActionView(mSearchView).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS
				| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW).setOnActionExpandListener(this).expandActionView();
		menu.add(0, 2, 0, R.string.text_crop_thumbnails).setCheckable(true)
				.setChecked(Preferences.isCropThumbnails(this));
		if (mKeepSearchFocus) mKeepSearchFocus = false; else mSearchView.clearFocus();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
			{
				finish();
				return true;
			}
			case 2:
			{
				Preferences.setCropThumbnails(this, !item.isChecked());
				mGridAdapter.notifyDataSetChanged();
				invalidateOptionsMenu();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		menu.add(0, 1, 0, R.string.text_download);
	}
	
	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case 1:
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				{
					if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
							!= PackageManager.PERMISSION_GRANTED)
					{
						requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
								info.position);
						return true;
					}
				}
				ImageItem imageItem = mGridAdapter.getItem(info.position);
				performDownload(imageItem);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			performDownload(mGridAdapter.getItem(requestCode));
		}
	}
	
	private void performDownload(ImageItem imageItem)
	{
		DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageItem.imageUriString));
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
				FileManager.obtainSimpleFileName(imageItem.imageUriString));
		request.allowScanningByMediaScanner();
		try
		{
			downloadManager.enqueue(request);
		}
		catch (IllegalArgumentException e)
		{
			String message = e.getMessage();
			if (message.equals("Unknown URL content://downloads/my_downloads"))
			{
				Toast.makeText(this, R.string.error_download_manager, Toast.LENGTH_LONG).show();
			}
			else throw e;
		}
	}
	
	private final Runnable mPositionRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			mGridView.setSelection(0);
		}
	};
	
	@Override
	public void onQueryLoadingSuccess(ArrayList<ImageItem> imageItems)
	{
		mImageItems = imageItems;
		mGridAdapter.setImageItems(imageItems);
		mGridView.post(mPositionRunnable);
	}
	
	@Override
	public void onQueryLoadingError(int errorMessageId)
	{
		Toast.makeText(this, getString(errorMessageId), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onImageLoadingSuccess(int contentId, String mimeType)
	{
		Uri uri = ImageProvider.build(contentId);
		setResult(RESULT_OK, new Intent().setDataAndType(uri, mimeType));
		finish();
	}
	
	@Override
	public void onImageLoadingError(int errorMessageId)
	{
		onQueryLoadingError(errorMessageId);
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		ImageItem imageItem = mGridAdapter.getItem(position);
		new LoadingDialog(null, imageItem.imageUriString, this).show(getFragmentManager(), LoadingDialog.TAG);
	}
}