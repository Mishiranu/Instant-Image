package com.mishiranu.instantimage;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Pair;

import com.mishiranu.instantimage.async.AsyncManager;
import com.mishiranu.instantimage.async.LoadImageTask;
import com.mishiranu.instantimage.async.LoadQueryTask;

public class LoadingDialog extends DialogFragment implements AsyncManager.Callback
{
	public static final String TAG = LoadingDialog.class.getName();

	private static final String EXTRA_QUERY = "query";
	private static final String EXTRA_IMAGE_URI_STRING = "imageUriString";
	private static final String TASK_LOAD = "load";
	
	public LoadingDialog()
	{
		
	}
	
	public LoadingDialog(String query, String imageUriString, Callback callback)
	{
		Bundle args = new Bundle();
		args.putString(EXTRA_QUERY, query);
		args.putString(EXTRA_IMAGE_URI_STRING, imageUriString);
		setArguments(args);
		setCallback(callback);
	}
	
	public static interface Callback
	{
		public void onQueryLoadingSuccess(ArrayList<ImageItem> imageItems);
		public void onQueryLoadingError(int errorMessageId);
		
		public void onImageLoadingSuccess(int contentId, String mimeType);
		public void onImageLoadingError(int errorMessageId);
	}
	
	private Callback mCallback;
	
	public void setCallback(Callback callback)
	{
		mCallback = callback;
	}
	
	@Override
	public void onDetach()
	{
		super.onDetach();
		mCallback = null;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(getString(R.string.text_loading));
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		HashMap<String, Object> extra = new HashMap<>();
		extra.put(EXTRA_QUERY, getArguments().getString(EXTRA_QUERY));
		extra.put(EXTRA_IMAGE_URI_STRING, getArguments().getString(EXTRA_IMAGE_URI_STRING));
		AsyncManager.get(this).startTask(TASK_LOAD, this, extra, false);
	}
	
	@Override
	public Pair<Object, AsyncManager.Holder> onCreateAndExecuteTask(String name, HashMap<String, Object> extra)
	{
		String query = (String) extra.get(EXTRA_QUERY);
		String imageUriString = (String) extra.get(EXTRA_IMAGE_URI_STRING);
		if (query != null)
		{
			LoadQueryTask task = new LoadQueryTask(query);
			task.executeOnExecutor(LoadQueryTask.THREAD_POOL_EXECUTOR);
			return task.getPair();
		}
		else if (imageUriString != null)
		{
			LoadImageTask task = new LoadImageTask(getActivity(), imageUriString);
			task.executeOnExecutor(LoadImageTask.THREAD_POOL_EXECUTOR);
			return task.getPair();
		}
		else return null;
	}
	
	@Override
	public void onFinishTaskExecution(String name, AsyncManager.Holder holder)
	{
		if (getArguments().getString(EXTRA_QUERY) != null)
		{
			int errorMessageId = holder.nextArgument();
			if (errorMessageId != 0) mCallback.onQueryLoadingError(errorMessageId); else
			{
				ArrayList<ImageItem> imageItems = holder.nextArgument();
				if (imageItems == null || imageItems.isEmpty()) mCallback.onQueryLoadingError(R.string.error_not_found);
				else mCallback.onQueryLoadingSuccess(imageItems);
			}
		}
		else if (getArguments().getString(EXTRA_IMAGE_URI_STRING) != null)
		{
			int errorMessageId = holder.nextArgument();
			int contentId = holder.nextArgument();
			String mimeType = holder.nextArgument();
			if (errorMessageId != 0) mCallback.onImageLoadingError(errorMessageId);
			else mCallback.onImageLoadingSuccess(contentId, mimeType);
		}
		dismissAllowingStateLoss();
	}
	
	@Override
	public void onRequestTaskCancel(String name, Object task)
	{
		if (task instanceof LoadQueryTask) ((LoadQueryTask) task).cancel(true);
		else if (task instanceof LoadImageTask) ((LoadImageTask) task).cancel(true);
	}
}