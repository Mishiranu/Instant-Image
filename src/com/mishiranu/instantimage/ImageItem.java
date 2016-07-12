package com.mishiranu.instantimage;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageItem implements Parcelable
{
	public final String imageUriString;
	public final String thumbnailUriString;
	public final String text;
	
	public ImageItem(String imageUriString, String thumbnailUriString, String text)
	{
		this.imageUriString = imageUriString;
		this.thumbnailUriString = thumbnailUriString;
		this.text = text;
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(imageUriString);
		dest.writeString(thumbnailUriString);
		dest.writeString(text);
	}
	
	public static final Parcelable.Creator<ImageItem> CREATOR = new Parcelable.Creator<ImageItem>()
	{
		@Override
		public ImageItem createFromParcel(Parcel source)
		{
			String imageUriString = source.readString();
			String thumbnailUriString = source.readString();
			String text = source.readString();
			return new ImageItem(imageUriString, thumbnailUriString, text);
		}
		
		@Override
		public ImageItem[] newArray(int size)
		{
			return new ImageItem[size];
		}
	};
}