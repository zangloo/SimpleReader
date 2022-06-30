package net.lzrj.SimpleReader.book;

import net.lzrj.SimpleReader.Reader;
import net.lzrj.SimpleReader.UString;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public class ContentBase implements Content
{
	private List<UString> lines;
	private int booksize;

	public ContentBase()
	{
		lines = new ArrayList<>();
		for (String l : Reader.ReaderTip)
			lines.add(new UString(l));
		booksize = calcSize();
	}

	public ContentBase(List<UString> content)
	{
		lines = content;
		booksize = calcSize();
	}

	public void setContent(List<UString> content)
	{
		lines = content;
		booksize = calcSize();
	}

	private int calcSize()
	{
		int s = 0;
		for (UString l : lines)
			s += l.length();
		return s;
	}

	@Override
	public UString line(int index)
	{
		return lines.get(index);
	}

	@Override
	public int lineCount()
	{
		return lines.size();
	}

	@Override
	public int size()
	{
		return booksize;
	}

	@Override
	public int size(int end)
	{
		if (end >= lines.size())
			return booksize;

		int s = 0;
		for (int i = 0; i < end; i++)
			s += lines.get(i).length();

		return s;
	}

	@Override
	public Position searchText(String txt, Position cpi)
	{
		for (int i = cpi.line; i < lineCount(); i++) {
			UString line = line(i);
			int pos = line.indexOf(txt, cpi.offset);
			if (pos >= 0) {
				cpi.line = i;
				cpi.offset = pos;
				return cpi;
			}
			cpi.offset = 0;
		}
		return null;
	}

	@Override
	public Position getPercentPos(int percent)
	{
		int p = size() * percent / 100;
		int c = 0, i;

		for (i = 0; i < lineCount(); i++) {
			c += line(i).length();
			if (c > p)
				break;
		}
		Position cpi = new Position();
		if (c > p) {
			cpi.line = i;
			cpi.offset = line(i).length() - (c - p);
		} else {
			cpi.line = i - 1;
			cpi.offset = 0;
		}
		return cpi;
	}

	@Override
	public boolean hasNotes()
	{
		return false;
	}

	@Override
	public String getNote(int line, int offset)
	{
		return null;
	}

	@Override
	public void clear()
	{
	}
}
