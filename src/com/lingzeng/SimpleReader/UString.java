package com.lingzeng.SimpleReader;

import com.lingzeng.SimpleReader.view.SimpleTextView;

/**
 * Created with IntelliJ IDEA.
 * User: zhanglu
 * Date: 12/21/12
 * Time: 1:41 PM
 * <p/>
 * this class replace standard string, for some book may contain code point big then 65535
 */
public class UString implements ContentLine
{
	private final String data;
	private final boolean allBMP;

	public UString(String str)
	{
		data = str;
		allBMP = (data.length() == data.codePointCount(0, data.length()));
	}

	public UString replaceChars(char[] oc, char[] nc)
	{
		char[] txt = data.toCharArray();
		SimpleTextView.replaceTextChar(txt, oc, nc);
		return new UString(String.valueOf(txt));
	}

	// index is code point based
	public int index16(int index)
	{
		if (allBMP)
			return index;
		return data.offsetByCodePoints(0, index);
	}

	// from, to is code point based
	public int count16(int from, int to)
	{
		if (allBMP)
			return to - from;
		return index16(to) - index16(from);
	}

	// from and to is utf-16 based
	public int count32(int from, int to)
	{
		if (allBMP)
			return to - from;
		return data.codePointCount(from, to);
	}

	public int length()
	{
		if (allBMP)
			return data.length();
		return data.codePointCount(0, data.length());
	}

	@Override
	public boolean isImage()
	{
		return false;
	}

	public int charAt(int index)
	{
		if (allBMP)
			return data.charAt(index);
		return data.codePointAt(index16(index));
	}

	public int indexOf(String str)
	{
		return indexOf(str, 0);
	}

	public int indexOf(String str, int from)
	{
		if (allBMP)
			return data.indexOf(str, from);
		int ret = data.indexOf(str, index16(from));
		if (ret < 0)
			return -1;
		return count32(0, ret);
	}

	public String substring(int from, int to)
	{
		if (allBMP)
			return data.substring(from, to);
		return data.substring(index16(from), index16(to));
	}

	public String substring(int from)
	{
		if (allBMP)
			return data.substring(from);
		return data.substring(index16(from));
	}

	@Override
	public String toString()
	{
		return data;
	}

	@Override
	public ContentLineType type()
	{
		return ContentLineType.text;
	}
}
