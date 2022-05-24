package net.lzrj.SimpleReader.book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午1:20
 */
public abstract class ChaptersBook extends Book
{
	protected ArrayList<TOCRecord> TOC = new ArrayList<>();
	protected int chapter;

	// return chapter count
	public int chapterCount()
	{
		return TOC.size();
	}

	// return chapter title at index
	public String readingTitle(int chapter, int line, int offset)
	{
		return TOC.get(tocIndex(chapter, line, offset)).title;
	}

	// return all chapter title
	public ArrayList<TOCRecord> getTOC()
	{
		return TOC;
	}

	// get current chapter index
	public int currChapter()
	{
		return chapter;
	}

	protected abstract boolean loadChapter(int index);
}
