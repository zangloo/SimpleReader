package net.lzrj.SimpleReader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import net.lzrj.SimpleReader.book.TextStyleType;
import net.lzrj.SimpleReader.view.SimpleTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhanglu
 * Date: 12/21/12
 * Time: 1:41 PM
 * <p/>
 * this class replace standard string, for some book may contain code point big then 65535
 */
public class UString
{
	public static class TextStyle
	{
		public final int from;
		public final int to;
		public final TextStyleType type;
		public final Object value;

		public TextStyle(int from, int to, TextStyleType type, Object value)
		{
			this.from = from;
			this.to = to;
			this.type = type;
			this.value = value;
		}

		@Override
		public String toString()
		{
			return type + " (" + from + " : " + to + "): " + (value == null ? "" : value.toString());
		}
	}

	public static class ImageValue
	{
		public final String href;
		private final byte[] bytes;
		private Bitmap image;

		public ImageValue(String href, byte[] bytes)
		{
			this.href = href;
			this.bytes = bytes;
		}

		public Bitmap getImage()
		{
			if (bytes == null)
				return null;
			if (image == null)
				image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			return image;
		}
	}

	private String data;
	private boolean allBMP;
	private boolean paragraph = false;
	private List<TextStyle> styles = new ArrayList<>();

	public UString(String str)
	{
		data = str.trim();
		allBMP = (data.length() == data.codePointCount(0, data.length()));
	}

	public UString replaceChars(char[] oc, char[] nc)
	{
		char[] txt = data.toCharArray();
		SimpleTextView.replaceTextChar(txt, oc, nc);
		return copy(this, new UString(String.valueOf(txt)));
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

	public void append(String other)
	{
		int length = other.codePointCount(0, other.length());
		if (length == 0)
			return;
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
		int fontSize = 100;
		for (int i = styles.size() - 1; i >= 0; i--) {
			TextStyle style = styles.get(i);
			if (TextStyleType.fontSize.equals(style.type) && style.from <= index && style.to > index)
				fontSize = (int) style.value * fontSize / 100;
		}
		return fontSize;
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

	public List<TextStyle> styles()
	{
		return styles;
	}

	public void concat(String string, HashMap<TextStyleType, Object> textStyles)
	{
		int from = length();
		append(string);
		if (textStyles != null) {
			int to = length();
			for (Map.Entry<TextStyleType, Object> entry : textStyles.entrySet())
				styles.add(new TextStyle(from, to, entry.getKey(), entry.getValue()));
		}
	}

	public boolean isParagraph()
	{
		return paragraph;
	}

	public void paragraph()
	{
		paragraph = true;
	}

	public void addStyle(int from, int to, TextStyleType type, Object value)
	{
		styles.add(new TextStyle(from, to, type, value));
	}

	private UString copy(UString orig, UString newOne)
	{
		newOne.styles = orig.styles;
		newOne.paragraph = orig.paragraph;
		return newOne;
	}
}
