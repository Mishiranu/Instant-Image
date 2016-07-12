package com.mishiranu.instantimage.widget;

import android.content.Context;
import android.widget.ImageView;

public class SquareImageView extends ImageView
{
	public SquareImageView(Context context)
	{
		super(context);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
	}
}