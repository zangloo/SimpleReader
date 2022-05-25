package net.lzrj.SimpleReader;

import net.lzrj.SimpleReader.book.TextStyleType;

import java.util.ArrayList;
import java.util.List;

public abstract class TextContentBase implements ContentLine
{
	public static class TextStyle
	{
		public final int from;
		public final int to;
		public final TextStyleType type;

		public TextStyle(int from, int to, TextStyleType type)
		{
			this.from = from;
			this.to = to;
			this.type = type;
		}

		@Override
		public String toString()
		{
			return type + " (" + from + " : " + to + ")";
		}
	}

	protected boolean paragraph = false;
	protected List<TextStyle> styles;

	public List<TextStyle> styles()
	{
		return styles;
	}

	public void concat(String string, TextStyleType textStyleType, int fontSize)
	{
		int from = length();
		append(string, fontSize);
		if (textStyleType != null) {
			int to = length();
			if (styles == null)
				styles = new ArrayList<>();
			styles.add(new TextStyle(from, to, textStyleType));
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

	@Override
	public ContentLineType type()
	{
		return ContentLineType.text;
	}

	@Override
	public boolean isImage()
	{
		return false;
	}

	abstract protected void append(String other, int fontSize);

	protected <T extends TextContentBase> T copy(T orig, T newOne)
	{
		newOne.styles = orig.styles;
		newOne.paragraph = orig.paragraph;
		return newOne;
	}
}
