package zhang.lu.SimpleReader.Book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午1:20
 */
public abstract class ChaptersBook extends Book
{
	protected ArrayList<TOCRecord> TOC = new ArrayList<TOCRecord>();
	protected int chapter;

	// return chapter count
	public int getChapterCount()
	{
		return TOC.size();
	}

	// return chapter title at index
	public String getChapterTitle(int index)
	{
		return TOC.get(index).title;
	}

	// return all chapter title
	public ArrayList<TOCRecord> getTOC()
	{
		return TOC;
	}

	// get current chapter index
	public int getCurrChapter()
	{
		return chapter;
	}

	public boolean gotoChapter(int index)
	{
		return !((index < 0) || (index >= getChapterCount()) || (index == getCurrChapter())) &&
			loadChapter(index);
	}

	protected abstract boolean loadChapter(int index);
}
