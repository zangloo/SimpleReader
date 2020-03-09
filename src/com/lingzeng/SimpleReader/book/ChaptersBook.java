package com.lingzeng.SimpleReader.book;

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
	public int chapterCount()
	{
		return TOC.size();
	}

	// return chapter title at index
	public String chapterTitle(int index)
	{
		return TOC.get(index).title;
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

	public boolean gotoChapter(int index)
	{
		return !((index < 0) || (index >= chapterCount()) || (index == currChapter())) &&
			loadChapter(index);
	}

	protected abstract boolean loadChapter(int index);
}
