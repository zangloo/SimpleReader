package zhang.lu.SimpleReader.book;

import android.graphics.Bitmap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public interface Content
{
	public static class ContentPosInfo
	{
		public int line, offset;
	}

	// has notes?
	public boolean hasNotes();

	// return null when no note at line:offset
	public String getNote(int line, int offset);

	// clear datas
	public void clear();

	// search txt from cpi
	public ContentPosInfo searchText(String txt, ContentPosInfo cpi);

	// get position info for <x>%
	public ContentPosInfo getPercentPos(int percent);

	// return image count
	public int imageCount();

	// return image for ImageContente
	public Bitmap image(int index);

	// return line at index
	public String line(int index);

	// return line count
	public int lineCount();

	// return book size from line[0] to line[end - 1]
	public int size(int end);

	// return book total size
	public int size();

}
