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
public class PlainTextContent extends BookContent
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

	protected void setContent(List<String> content)
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

	@Override
	public String line(int index)
	{
		return lines.get(index);
	}

	@Override
	public int getLineCount()
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
}
