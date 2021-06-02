package com.lingzeng.SimpleReader;

import android.graphics.Bitmap;

public abstract class ContentImage implements ContentLine
{
	protected final String ref;

	public ContentImage(String ref)
	{
		this.ref = ref;
	}

	@Override
	public ContentLineType type()
	{
		return ContentLineType.image;
	}

	@Override
	public int length()
	{
		return 0;
	}

	abstract public Bitmap getImage();
}
