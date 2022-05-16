package net.lzrj.SimpleReader;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class TextContentBase implements ContentLine
{
	protected boolean paragraph = false;
	protected List<Pair<Integer, Integer>> underlines;

	public List<Pair<Integer, Integer>> underlines()
	{
		return underlines;
	}

	public void concat(String string, boolean underline, int fontSizeDelta)
	{
		int from = length();
		append(string, fontSizeDelta);
		if (underline) {
			int to = length();
			if (underlines == null)
				underlines = new ArrayList<>();
			underlines.add(Pair.create(from, to));
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

	abstract protected void append(String other, int fontSizeDelta);

	protected <T extends TextContentBase> T copy(T orig, T newOne)
	{
		newOne.underlines = orig.underlines;
		newOne.paragraph = orig.paragraph;
		return newOne;
	}
}
