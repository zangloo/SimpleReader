package com.lingzeng.SimpleReader;

import android.graphics.Bitmap;

public abstract class ImageContent implements ContentLine
{
	@Override
	public ContentLineType type()
	{
		return ContentLineType.image;
	}

	@Override
	public int length()
	{
		return 1;
	}

	abstract public Bitmap getImage();
}
