package com.mishiranu.instantimage;

import java.util.ArrayList;

import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mishiranu.instantimage.util.Preferences;
import com.mishiranu.instantimage.widget.SquareImageView;
import com.squareup.picasso.Picasso;

public class GridAdapter extends BaseAdapter
{
	private final ArrayList<ImageItem> mImageItems = new ArrayList<>();
	
	public void setImageItems(ArrayList<ImageItem> imageItems)
	{
		mImageItems.clear();
		if (imageItems != null) mImageItems.addAll(imageItems);
		notifyDataSetChanged();
	}
	
	private static class ViewHolder
	{
		SquareImageView imageView;
		TextView textView;
	}
	
	private static final int[] ATTRS = {android.R.attr.selectableItemBackground};
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder viewHolder;
		if (convertView == null)
		{
			float density = parent.getResources().getDisplayMetrics().density;
			FrameLayout frameLayout = new FrameLayout(parent.getContext());
			frameLayout.setBackgroundColor(0xff7f7f7f);
			TypedArray typedArray = parent.getContext().obtainStyledAttributes(ATTRS);
			frameLayout.setForeground(typedArray.getDrawable(0));
			typedArray.recycle();
			viewHolder = new ViewHolder();
			viewHolder.imageView = new SquareImageView(parent.getContext());
			frameLayout.addView(viewHolder.imageView, FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);
			viewHolder.textView = new TextView(parent.getContext());
			viewHolder.textView.setBackgroundColor(0xcc222222);
			int padding = (int) (8 * density + 0.5f);
			viewHolder.textView.setPadding(padding, padding, padding, padding);
			viewHolder.textView.setSingleLine(true);
			viewHolder.textView.setEllipsize(TextUtils.TruncateAt.END);
			viewHolder.textView.setTextColor(0xffffffff);
			viewHolder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			frameLayout.addView(viewHolder.textView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
			frameLayout.setTag(viewHolder);
			convertView = frameLayout;
		}
		else viewHolder = (ViewHolder) convertView.getTag();
		ImageItem imageItem = getItem(position);
		viewHolder.imageView.setScaleType(Preferences.isCropThumbnails(parent.getContext())
				? SquareImageView.ScaleType.CENTER_CROP : SquareImageView.ScaleType.FIT_CENTER);
		Picasso.with(parent.getContext()).load(imageItem.thumbnailUriString).into(viewHolder.imageView);
		viewHolder.textView.setText(imageItem.text);
		return convertView;
	}
	
	@Override
	public long getItemId(int position)
	{
		return 0;
	}
	
	@Override
	public ImageItem getItem(int position)
	{
		return mImageItems.get(position);
	}
	
	@Override
	public int getCount()
	{
		return mImageItems.size();
	}
}