package zhang.lu.SimpleReader.Book;

import android.graphics.Bitmap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public abstract class BookContent
{
	public static class ContentPosInfo
	{
		public int line, offset;
	}

	public static enum Type
	{
		text, image
	}

	// content type
	public Type type()
	{
		return Type.text;
	}

	// has notes?
	public boolean hasNotes()
	{
		return false;
	}

	// return null when no note at line:offset
	public String getNote(int line, int offset)
	{
		return null;
	}

	// clear datas
	public void clear()
	{
	}

	// search txt from cpi
	public ContentPosInfo searchText(String txt, ContentPosInfo cpi)
	{
		for (int i = cpi.line; i < getLineCount(); i++) {
			int pos = line(i).indexOf(txt, cpi.offset);
			if (pos >= 0) {
				cpi.line = i;
				cpi.offset = pos;
				return cpi;
			}
			cpi.offset = 0;
		}
		return null;
	}

	// get position info for <x>%
	public ContentPosInfo getPercentPos(int percent)
	{
		int p = size() * percent / 100;
		int c = 0, i;

		for (i = 0; i < getLineCount(); i++) {
			c += line(i).length();
			if (c > p)
				break;
		}
		ContentPosInfo cpi = new ContentPosInfo();
		if (c > p) {
			cpi.line = i;
			cpi.offset = line(i).length() - (c - p);
		} else {
			cpi.line = i - 1;
			cpi.offset = 0;
		}
		return cpi;
	}

	// return image count
	public int imageCount()
	{
		return 0;
	}

	// return image for ImageContente
	public Bitmap image(int index)
	{
		return null;
	}

	// return line at index
	public abstract String line(int index);

	// return line count
	public abstract int getLineCount();

	// return book size from line[0] to line[end - 1]
	public abstract int size(int end);

	// return book total size
	public abstract int size();

}
