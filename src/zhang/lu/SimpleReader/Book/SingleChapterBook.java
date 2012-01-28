package zhang.lu.SimpleReader.Book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午8:37
 */
public class SingleChapterBook extends Book
{
	BookContent content = null;

	SingleChapterBook(BookContent c)
	{
		content = c;
	}

	@Override
	public int getChapterCount()
	{
		return 1;
	}

	@Override
	public String getChapterTitle(int index)
	{
		return null;
	}

	@Override
	public ArrayList<TOCRecord> getTOC()
	{
		return null;
	}

	@Override
	public int getCurrChapter()
	{
		return 0;
	}

	@Override
	protected boolean loadChapter(int index)
	{
		return false;
	}

	@Override
	public BookContent getContent(int index)
	{
		return content;
	}

	@Override
	public void close()
	{
		content.clear();
	}
}
