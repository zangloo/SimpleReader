package net.lzrj.SimpleReader.book;

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
	public abstract int chapterCount();

	// return chapter title at index
	public String readingTitle(int chapter, int line, int offset)
	{
		return null;
	}

	// return all chapter title
	public abstract ArrayList<TOCRecord> getTOC();

	// toc index of current chapter
	public int tocIndex(int chapter, int line, int offset)
	{
		return chapter;
	}

	// goto position of toc[index]
	public Content.Position gotoToc(int tocIndex)
	{
		gotoChapter(tocIndex);
		return new Content.Position(0, 0);
	}

	// goto position of [target#anchor]
	public Content.Position gotoLink(String href)
	{
		return null;
	}

	// get current chapter index
	public abstract int currChapter();

	// load chapter info
	protected abstract boolean loadChapter(int index);

	// get current chapter content
	public Content content()
	{
		return content(currChapter());
	}

	// get chapter content
	public abstract Content content(int index);

	// switch to chapter index
	public boolean gotoChapter(int index)
	{
		return !((index < 0) || (index >= chapterCount()) || (index == currChapter())) &&
			loadChapter(index);
	}

	// close book
	public void close()
	{
	}
}
