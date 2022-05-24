package net.lzrj.SimpleReader.book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午8:37
 */
public class SingleChapterBook extends Book
{
	Content content = null;

	public SingleChapterBook(Content c)
	{
		content = c;
	}

	@Override
	public int chapterCount()
	{
		return 1;
	}

	@Override
	public ArrayList<TOCRecord> getTOC()
	{
		return null;
	}

	@Override
	public int currChapter()
	{
		return 0;
	}

	@Override
	protected boolean loadChapter(int index)
	{
		return false;
	}

	@Override
	public Content content(int index)
	{
		return content;
	}

	@Override
	public void close()
	{
		content.clear();
	}
}
