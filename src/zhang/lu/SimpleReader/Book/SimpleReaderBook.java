package zhang.lu.SimpleReader.Book;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-6
 * Time: 下午8:21
 */
public class SimpleReaderBook extends BookContent implements BookLoader.Loader
{
	public static final String[] suffixes = {"srb"};
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

	// get the min line index of the <chapter> lines for <chapter> that size big then <size>
	// return index, line text, size
	private static final String configPosSQL = "selectPosSQL";

	// get lines count for <chapter>
	// return count
	private static final String configCountSQL = "selectCountSQL";

	// search <chapter> lines for <chapter> from <index> for <string>
	// return line index and line text
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

	private SQLiteDatabase db;
	//private int version;
	private int indexBase;
	private boolean hasNotes;
	private char markChar;
	private String lineSQL;
	private String noteSQL;
	private String sizeSQL;
	private String posSQL;
	private String searchSQL;
	private String chapterSQL;
	private String chapterListSQL;
	private String countSQL;
	private int chapterCount;
	private int booksize;
	private int lineCount;
	private int chapter;

	// cache size
	private static final int LINE_CACHE_SIZE = 90;
	// fetch lines from (<index> - LINE_CACHE_PREFETCH_SIZE) to (<index> + LINE_CACHE_SIZE - 1)
	private static final int LINE_CACHE_PREFETCH_SIZE = 10;
	private HashMap<Integer, String> lineCache = new HashMap<Integer, String>();

	public String[] getSuffixes()
	{
		return suffixes;
	}

	public BookContent load(VFile f)
	{
		db = SQLiteDatabase.openDatabase(f.getPath(), null,
						 SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		HashMap<String, String> map = new HashMap<String, String>();
		Cursor cursor = db.query(INFO_TABLE_NAME, INFO_TABLE_COLS, null, null, null, null, null);
		while (cursor.moveToNext())
			map.put(cursor.getString(0), cursor.getString(1));
		cursor.close();

		//version = new Integer(map.get(configVersion));
		indexBase = new Integer(map.get(configIndexBase));
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
			return null;
		}
		chapterCount = cursor.getInt(0);
		cursor.close();
		if (chapterCount == 0)
			return null;

		chapter = indexBase;
		lineCount = selectLineCount();
		if (lineCount == 0)
			return null;

		booksize = selectSize(lineCount);
		if (booksize == 0)
			return null;

		lineCache.clear();
		return this;
	}

	public String line(int index)
	{
		if (lineCache.containsKey(index))
			return lineCache.get(index);
		int lineno = index + indexBase;
		int b = Math.max(lineno - LINE_CACHE_PREFETCH_SIZE, indexBase);
		int e = Math.min(lineno + LINE_CACHE_SIZE, lineCount + indexBase - 1);

		Cursor c = db
			.rawQuery(lineSQL, new String[]{String.valueOf(chapter), String.valueOf(b), String.valueOf(e)});
		int i = b - indexBase;
		while (c.moveToNext()) {
			if (!lineCache.containsKey(i))
				lineCache.put(i, c.getString(0));
			i++;
		}
		c.close();
		return lineCache.get(index);
	}

	public int getLineCount()
	{
		return lineCount;
	}

	public int size(int end)
	{
		if (end == 0)
			return 0;
		if (end >= lineCount)
			return booksize;

		return selectSize(end);
	}

	public int size()
	{
		return booksize;
	}

	public String getNote(int index, int offset)
	{
		if (!hasNotes)
			return null;
		if (index >= lineCount)
			return null;
		String l = line(index);
		String n = null;
		if (offset >= l.length())
			return null;
		if (l.charAt(offset) != markChar)
			return null;
		Cursor c = db.rawQuery(noteSQL,
				       new String[]{String.valueOf(chapter), String.valueOf(index + indexBase), String
					       .valueOf(offset)});
		if (c.moveToFirst())
			n = c.getString(0);
		c.close();
		return n;
	}

	public void unload(BookContent book)
	{
		db.close();
		lineCache.clear();
	}

	@Override
	public ContentPosInfo searchText(String txt, ContentPosInfo cpi)
	{
		if (cpi.offset > 0) {
			if ((cpi.offset = line(cpi.line).indexOf(txt, cpi.offset)) >= 0)
				return cpi;
			cpi.line++;
		}

		Cursor c = db.rawQuery(searchSQL, new String[]{String.valueOf(chapter), String.valueOf(chapter), String
			.valueOf(cpi.line + indexBase), txt});
		if (c.moveToFirst()) {
			cpi.line = c.getInt(0) - indexBase;
			cpi.offset = c.getString(1).indexOf(txt);
		} else
			cpi = null;
		c.close();

		return cpi;
	}

	@Override
	public ContentPosInfo getPercentPos(int percent)
	{
		int p = booksize * percent / 100;
		ContentPosInfo cpi = new ContentPosInfo();

		Cursor c = db.rawQuery(posSQL, new String[]{String.valueOf(chapter), String.valueOf(chapter), String
			.valueOf(p)});
		if (!c.moveToFirst()) {
			cpi.line = 0;
			cpi.offset = 0;
		} else {
			cpi.line = c.getInt(0) - indexBase;
			String l = c.getString(1);
			cpi.offset = p - (c.getInt(2) - l.length());
		}
		c.close();
		return cpi;
	}

	@Override
	public int getChapterCount()
	{
		return chapterCount;
	}

	@Override
	public String getChapterTitle()
	{
		return getChapterTitle(getCurrChapter());
	}

	@Override
	public String getChapterTitle(int index)
	{
		Cursor c = db.rawQuery(chapterSQL, new String[]{String.valueOf(index + indexBase)});
		if (!c.moveToFirst()) {
			c.close();
			return "";
		}
		String s = c.getString(0);
		c.close();

		return s;
	}

	@Override
	public ArrayList<String> getChapterTitleList()
	{
		ArrayList<String> l = new ArrayList<String>(getChapterCount());
		Cursor c = db.rawQuery(chapterListSQL, null);
		while (c.moveToNext())
			l.add(c.getString(0));
		c.close();
		return l;
	}

	@Override
	public int getCurrChapter()
	{
		return chapter - indexBase;
	}

	@Override
	public boolean gotoChapter(int index)
	{
		if (index < 0 || index >= chapterCount)
			return false;

		if (chapter == index + indexBase)
			return false;

		chapter = index + indexBase;
		updateValues();
		return true;
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
}
