package zhang.lu.SimpleReader.Book;

import zhang.lu.SimpleReader.Reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public class PlainTextContent implements BookContent
{
	private List<String> lines;
	private int booksize = 0;

	public PlainTextContent()
	{
		lines = new ArrayList<String>();
		Collections.addAll(lines, Reader.ReaderTip);
		booksize = calcSize();
	}

	public PlainTextContent(List<String> content)
	{
		lines = content;
		booksize = calcSize();
	}

	private int calcSize()
	{
		int s = 0;
		for (String l : lines)
			s += l.length();
		return s;
	}

	public String line(int index)
	{
		return lines.get(index);
	}

	public int getLineCount()
	{
		return lines.size();
	}

	public int size()
	{
		return booksize;
	}

	public int size(int end)
	{
		if (end >= lines.size())
			return booksize;

		int s = 0;
		for (int i = 0; i < end; i++)
			s += lines.get(i).length();

		return s;
	}

	public String getNote(int line, int offset)
	{
		return null;
	}

	public void close()
	{
	}

	public ContentPosInfo searchText(String txt, ContentPosInfo cpi)
	{
		for (int i = cpi.line; i < lines.size(); i++) {
			int pos = lines.get(i).indexOf(txt, cpi.offset);
			if (pos >= 0) {
				cpi.line = i;
				cpi.offset = pos;
				return cpi;
			}
			cpi.offset = 0;
		}
		return null;
	}

	public ContentPosInfo getPercentPos(int percent)
	{
		int p = booksize * percent / 100;
		int c = 0, i;

		for (i = 0; i < lines.size(); i++) {
			c += lines.get(i).length();
			if (c > p)
				break;
		}
		ContentPosInfo cpi = new ContentPosInfo();
		if (c > p) {
			cpi.line = i;
			cpi.offset = lines.get(i).length() - (c - p);
		} else {
			cpi.line = i - 1;
			cpi.offset = 0;
		}
		return cpi;
	}
}
