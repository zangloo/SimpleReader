package zhang.lu.SimpleReader.Book;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public interface BookContent
{
	class ContentPosInfo
	{
		public int line, offset;
	}

	public String line(int index);

	// return chapter count
	public int getChapterCount();

	// return current chapter title
	public String getChapterTitle(int index);

	// return all chapter title
	public ArrayList<String> getChapterTitleList();

	// get current chapter index
	public int getCurrChapter();

	// goto prev chapter
	public boolean prevChapter();

	// goto next chapter
	public boolean nextChapter();

	// switch to chapter index
	public boolean gotoChapter(int index);

	// return line count
	public int getLineCount();

	// return book size from line[0] to line[end - 1]
	public int size(int end);

	// return book total size
	public int size();

	// return null when no note at line:offset
	public String getNote(int line, int offset);

	// search txt from cpi
	public ContentPosInfo searchText(String txt, ContentPosInfo cpi);

	// get position info for <x>%
	public ContentPosInfo getPercentPos(int percent);
}
