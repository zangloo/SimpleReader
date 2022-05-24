package net.lzrj.SimpleReader.book.SimpleReader;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.Content;
import net.lzrj.SimpleReader.book.TOCRecord;
import net.lzrj.SimpleReader.vfs.VFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:21
 */
public class SimpleReaderBook extends net.lzrj.SimpleReader.book.Book implements Content
{
	public static final String[] INFO_TABLE_COLS = new String[]{"key", "value"};
	public static final String INFO_TABLE_NAME = "info";

	//private static final String configVersion = "version";
	private static final String configHasNotes = "hasNotes";
	private static final String configIndexBase = "indexBase";
	private static final String configNoteMarkChar = "noteMark";

	// select lines for <chapter> from <begin> to <end> index
	// return line string
	private static final String configLineSQL = "selectLineSQL";

	// select note for <chapter> <line> and <offset>
	// return note string
	private static final String configNoteSQL = "selectNoteSQL";

	// select size sum for <chapter> from line 1 to <index>
	// return size
	private static final String configSizeSQL = "selectSizeSQL";

	// get the line index for <chapter> that size big then <size>
	// return index
	private static final String configPosSQL = "selectPosSQL";

	// get lines count for <chapter>
	// return count
	private static final String configCountSQL = "selectCountSQL";

	// search for <chapter> from <index> for <string>
	// return line index
	private static final String configSearchSQL = "selectSearchSQL";

	// get chapter <index> title
	// return chapter title
	private static final String configChapterSQL = "selectChapterSQL";

	// get all chapter title
	// return chapter titles
	private static final String configChapterListSQL = "selectChapterListSQL";

	// get chapter count
	// return count
	private static final String configChapterCountSQL = "selectChapterCountSQL";

	private final SQLiteDatabase db;
	//private int version;
	private final int indexBase;
	private final boolean hasNotes;
	private final char markChar;
	private final String lineSQL;
	private final String noteSQL;
	private final String sizeSQL;
	private final String posSQL;
	private final String searchSQL;
	private final String chapterSQL;
	private final String chapterListSQL;
	private final String countSQL;
	private final int chapterCount;
	private int booksize;
	private int lineCount;
	private int chapter;

	// cache size
	private static final int LINE_CACHE_SIZE = 90;
	// fetch lines from (<index> - LINE_CACHE_PREFETCH_SIZE) to (<index> + LINE_CACHE_SIZE - 1)
	private static final int LINE_CACHE_PREFETCH_SIZE = 10;
	private final HashMap<Integer, UString> lineCache = new HashMap<>();

	SimpleReaderBook(VFile f, Config.ReadingInfo ri) throws IOException
	{
		db = SQLiteDatabase.openDatabase(f.getRealPath(), null,
			SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		HashMap<String, String> map = new HashMap<>();
		Cursor cursor = db.query(INFO_TABLE_NAME, INFO_TABLE_COLS, null, null, null, null, null);
		while (cursor.moveToNext())
			map.put(cursor.getString(0), cursor.getString(1));
		cursor.close();

		//version = new Integer(map.get(configVersion));
		indexBase = Integer.parseInt(map.get(configIndexBase));
		hasNotes = map.get(configHasNotes).equals("true");
		markChar = map.get(configNoteMarkChar).charAt(0);
		lineSQL = map.get(configLineSQL);
		noteSQL = map.get(configNoteSQL);
		sizeSQL = map.get(configSizeSQL);
		posSQL = map.get(configPosSQL);
		searchSQL = map.get(configSearchSQL);
		chapterSQL = map.get(configChapterSQL);
		String chapterCountSQL = map.get(configChapterCountSQL);
		countSQL = map.get(configCountSQL);
		chapterListSQL = map.get(configChapterListSQL);

		cursor = db.rawQuery(chapterCountSQL, null);
		if (!cursor.moveToFirst()) {
			cursor.close();
			throw new IOException("Format incorrect");
		}
		chapterCount = cursor.getInt(0);
		cursor.close();
		if (chapterCount == 0)
			throw new IOException("Format incorrect");

		if (ri.chapter >= chapterCount)
			throw new IOException(
				String.format("Chapter %d not exist, load failed", ri.chapter + indexBase));
		chapter = ri.chapter + indexBase;

		lineCount = selectLineCount();
		if (lineCount == 0)
			throw new IOException("Format incorrect");

		booksize = selectSize(lineCount);
		if (booksize == 0)
			throw new IOException("Format incorrect");

		lineCache.clear();
	}

	@Override
	public int chapterCount()
	{
		return chapterCount;
	}

	@Override
	public String readingTitle(int chapter, int line, int offset)
	{
		Cursor c = db.rawQuery(chapterSQL, new String[]{String.valueOf(chapter + indexBase)});
		if (!c.moveToFirst()) {
			c.close();
			return "";
		}
		String s = c.getString(0);
		c.close();

		return s;
	}

	@Override
	public ArrayList<TOCRecord> getTOC()
	{
		ArrayList<TOCRecord> l = new ArrayList<>(chapterCount());
		Cursor c = db.rawQuery(chapterListSQL, null);
		while (c.moveToNext())
			l.add(new TOCRecord(c.getString(0)));
		c.close();
		return l;
	}

	@Override
	public int currChapter()
	{
		return chapter - indexBase;
	}

	@Override
	public boolean loadChapter(int index)
	{
		chapter = index + indexBase;
		updateValues();
		return true;
	}

	@Override
	public Content content(int index)
	{
		return this;
	}

	private void updateValues()
	{
		lineCache.clear();
		lineCount = selectLineCount();
		booksize = selectSize(lineCount);
	}

	private int selectLineCount()
	{
		Cursor cursor = db.rawQuery(countSQL, new String[]{String.valueOf(chapter)});

		if (!cursor.moveToNext()) {
			cursor.close();
			return 0;
		}
		int cnt = cursor.getInt(0);
		cursor.close();
		return cnt;
	}

	private int selectSize(int end)
	{
		Cursor c = db
			.rawQuery(sizeSQL, new String[]{String.valueOf(chapter), String.valueOf(end + indexBase - 1)});
		if (!c.moveToFirst()) {
			c.close();
			return 0;
		}
		int s = c.getInt(0);
		c.close();

		return s;
	}

	public void close()
	{
		db.close();
		lineCache.clear();
	}

	@Override
	public UString line(int index)
	{
		if (lineCache.containsKey(index))
			return lineCache.get(index);
		int lineno = index + indexBase;
		int b = Math.max(lineno - LINE_CACHE_PREFETCH_SIZE, indexBase);
		int e = Math.min(lineno + LINE_CACHE_SIZE, lineCount + indexBase - 1);

		Cursor c = db.rawQuery(lineSQL, new String[]{String.valueOf(chapter), String.valueOf(b), String
			.valueOf(e)});
		int i = b - indexBase;
		while (c.moveToNext()) {
			if (!lineCache.containsKey(i))
				lineCache.put(i, new UString(c.getString(0)));
			i++;
		}
		c.close();
		return lineCache.get(index);
	}

	@Override
	public UString text(int index)
	{
		return line(index);
	}

	@Override
	public int lineCount()
	{
		return lineCount;
	}

	@Override
	public int size(int end)
	{
		if (end == 0)
			return 0;
		if (end >= lineCount)
			return booksize;

		return selectSize(end);
	}

	@Override
	public int size()
	{
		return booksize;
	}

	@Override
	public boolean hasNotes()
	{
		return hasNotes;
	}

	@Override
	public String getNote(int index, int offset)
	{
		if (!hasNotes)
			return null;
		if (index >= lineCount)
			return null;
		UString l = line(index);
		String n = null;
		if (offset >= l.length())
			return null;
		if (l.charAt(offset) != markChar)
			return null;
		Cursor c = db.rawQuery(noteSQL, new String[]{String.valueOf(chapter), String
			.valueOf(index + indexBase), String.valueOf(offset)});
		if (c.moveToFirst())
			n = c.getString(0);
		c.close();
		return n;
	}

	@Override
	public void clear()
	{
	}

	@Override
	public Position searchText(String txt, Position cpi)
	{
		if (cpi.offset > 0) {
			if ((cpi.offset = line(cpi.line).indexOf(txt, cpi.offset)) >= 0)
				return cpi;
			cpi.line++;
		}

		int idx = indexBase - 1;
		Cursor c = db.rawQuery(searchSQL, new String[]{String.valueOf(chapter), String
			.valueOf(cpi.line + indexBase), txt});
		if (c.moveToFirst())
			idx = c.getInt(0);
		c.close();
		if (idx < indexBase)
			return null;

		idx -= indexBase;
		UString line = line(idx);
		cpi.line = idx;
		cpi.offset = line.indexOf(txt);

		return cpi;
	}

	@Override
	public Position getPercentPos(int percent)
	{
		int p = booksize * percent / 100;
		Position cpi = new Position();

		int idx = indexBase - 1;
		Cursor c = db.rawQuery(posSQL, new String[]{String.valueOf(chapter), String.valueOf(p)});
		if (!c.moveToFirst()) {
			cpi.line = 0;
			cpi.offset = 0;
		} else
			idx = c.getInt(0);
		c.close();
		if (idx < indexBase)
			return cpi;

		idx -= indexBase;
		cpi.line = idx;
		UString l = line(idx);
		cpi.offset = p - (size(idx + 1) - l.length());
		return cpi;
	}
}
