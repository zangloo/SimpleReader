package com.lingzeng.SimpleReader;

public interface ContentLine
{
	ContentLineType type();

	int length();

	boolean isImage();
}
