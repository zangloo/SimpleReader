package zhang.lu.SimpleReader.book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午2:12
 */
public abstract class Book
{
	// return chapter count
	public abstract int getChapterCount();

	// return current chapter title at index
	public String getChapterTitle()
	{
		return getChapterTitle(getCurrChapter());
	}

	// return chapter title at index
	public abstract String getChapterTitle(int index);

	// return all chapter title
	public abstract ArrayList<TOCRecord> getTOC();

	// get current chapter index
	public abstract int getCurrChapter();

	// load chapter info
	protected abstract boolean loadChapter(int index);

	// get current chapter content
	public Content getContent()
	{
		return getContent(getCurrChapter());
	}

	// get chapter content
	public abstract Content getContent(int index);

	// switch to chapter index
	public boolean gotoChapter(int index)
	{
		return !((index < 0) || (index >= getChapterCount()) || (index == getCurrChapter())) &&
			loadChapter(index);
	}

	// book's chapters has level
	public boolean hasLevel()
	{
		return false;
	}

	// close book
	public abstract void close();
}
