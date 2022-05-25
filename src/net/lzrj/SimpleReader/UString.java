package net.lzrj.SimpleReader;

import net.lzrj.SimpleReader.view.SimpleTextView;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: zhanglu
 * Date: 12/21/12
 * Time: 1:41 PM
 * <p/>
 * this class replace standard string, for some book may contain code point big then 65535
 */
public class UString extends TextContentBase
{
	private String data;
	private boolean allBMP;
	private final Vector<int[]> fontSizes = new Vector<>();

	public UString(String str)
	{
		this(str, 100);
	}

	public UString(String str, int fontSize)
	{
		data = str.trim();
		allBMP = (data.length() == data.codePointCount(0, data.length()));
		int length = this.length();
		if (length > 0)
			fontSizes.add(new int[]{length, fontSize});
	}

	public UString replaceChars(char[] oc, char[] nc)
	{
		char[] txt = data.toCharArray();
		SimpleTextView.replaceTextChar(txt, oc, nc);
		UString ret = copy(this, new UString(String.valueOf(txt)));
		ret.fontSizes.clear();
		ret.fontSizes.addAll(this.fontSizes);
		return ret;
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

	@Override
	public void append(String other, int fontSize)
	{
		int length = other.codePointCount(0, other.length());
		if (length == 0)
			return;
		if (fontSizes.size() > 0 && fontSizes.lastElement()[1] == fontSize)
			fontSizes.lastElement()[0] += length;
		else {
			int currentLen = this.length();
			fontSizes.add(new int[]{currentLen + length, fontSize});
		}
		data = data + other;
		allBMP = allBMP && (other.length() == length);
	}

	public int charAt(int index)
	{
		if (allBMP)
			return data.charAt(index);
		return data.codePointAt(index16(index));
	}

	public int charSizeAt(int index)
	{
		for (int[] range : fontSizes) {
			if (range[0] > index)
				return range[1];
		}
		return 0;
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

	public String text()
	{
		return data;
	}

	@Override
	public ContentLineType type()
	{
		return ContentLineType.text;
	}
}
