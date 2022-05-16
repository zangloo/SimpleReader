package net.lzrj.SimpleReader.book;

import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.UString;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:32
 */
public interface Content
{
	class Position
	{
		public int line, offset;

		public Position()
		{
			line = offset = 0;
		}

		public Position(int line, int offset)
		{
			this.line = line;
			this.offset = offset;
		}
	}

	// has notes?
	boolean hasNotes();

	// return null when no note at line:offset
	String getNote(int line, int offset);

	// clear datas
	void clear();

	// search txt from cpi
	Position searchText(String txt, Position cpi);

	// get position info for <x>%
	Position getPercentPos(int percent);

	// return line at index
	ContentLine line(int index);

	// return text at index
	UString text(int index);

	// return line count
	int lineCount();

	// return book size from line[0] to line[end - 1]
	int size(int end);

	// return book total size
	int size();
}
