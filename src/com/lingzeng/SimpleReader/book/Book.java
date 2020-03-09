package com.lingzeng.SimpleReader.book;

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

	// return current chapter title at index
	public String chapterTitle()
	{
		return chapterTitle(currChapter());
	}

	// return chapter title at index
	public abstract String chapterTitle(int index);

	// return all chapter title
	public abstract ArrayList<TOCRecord> getTOC();

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
	public abstract void close();
}
