package zhang.lu.SimpleReader.book;

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
	public Content getContent(int index)
	{
		return content;
	}

	@Override
	public void close()
	{
		content.clear();
	}
}