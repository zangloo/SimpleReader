package zhang.lu.SimpleReader.book;

import android.graphics.Bitmap;
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
public class PlainTextContent implements Content
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

	public void setContent(List<String> content)
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
	public ContentPosInfo searchText(String txt, ContentPosInfo cpi)
	{
		for (int i = cpi.line; i < lineCount(); i++) {
			int pos = line(i).indexOf(txt, cpi.offset);
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
	public ContentPosInfo getPercentPos(int percent)
	{
		int p = size() * percent / 100;
		int c = 0, i;

		for (i = 0; i < lineCount(); i++) {
			c += line(i).length();
			if (c > p)
				break;
		}
		ContentPosInfo cpi = new ContentPosInfo();
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
	public int imageCount() { return 0; }

	@Override
	public Bitmap image(int index) { return null; }

	@Override
	public boolean hasNotes() { return false; }

	@Override
	public String getNote(int line, int offset) { return null; }

	@Override
	public void clear() {}
}
