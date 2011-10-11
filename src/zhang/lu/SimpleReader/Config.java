package zhang.lu.SimpleReader;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.jetbrains.annotations.Nullable;
import zhang.lu.SimpleReader.Book.VFile;
import zhang.lu.SimpleReader.Popup.BookmarkManager;
import zhang.lu.SimpleReader.View.SimpleTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-7
 * Time: 下午3:27
 */

public class Config extends SQLiteOpenHelper
{
	public static final String configDB = "SimpleReader.config.sqlite";

	public static final int MAX_RECENTLY_FILE_COUNT = 20;
	public static final String RECENTLY_FILE_PREFIX = "recentfile";

	public enum GestureDirect
	{
		up(R.string.gesture_up_label), down(R.string.gesture_down_label),
		right(R.string.gesture_right_label), left(R.string.gesture_left_label),
		clickUp(R.string.gesture_click_up_label), clickDown(R.string.gesture_click_down_label),
		clickRight(R.string.gesture_click_right_label), clickLeft(R.string.gesture_click_left_label);

		private final int value;
		private String string = null;

		GestureDirect(int v)
		{
			value = v;
		}

		public int v() { return value; }

		@Override
		public String toString() { return string; }

		public String s() { return super.toString(); }
	}

	public static class ReadingInfo
	{
		public String name;
		public int chapter;
		public int line, offset;
		public int percent;
		private long id;

		public long getID() {return id;}
	}

	private static final int DB_VERSION = 1;

	private static final String CONFIG_TABLE_NAME = "config";
	private static final String[] CONFIG_TABLE_COLS = {"key", "value"};
	private static final String BOOK_INFO_TABLE_NAME = "bookinfo";
	//last reading status, percent is reserved, rowid for link with bookmark
	private static final String[] BOOK_INFO_TABLE_COLS = {"name", "chapter", "line", "offset", "percent", "rowid"};
	private static final String BOOKMARKS_TABLE_NAME = "bookmark";
	private static final String[] BOOKMARKS_TABLE_COLS = {"bookrowid", "desc", "chapter", "line", "offset", "rowid"};

	private static final String configCurrFile = "currfile";
	private static final String configFontSize = "fontsize";
	private static final String configColor = "color";
	private static final String configBColor = "bcolor";
	private static final String configNightColor = "ncolor";
	private static final String configNightBColor = "nbcolor";
	private static final String configColorBright = "colorbright";
	private static final String configHanStyle = "han";
	private static final String configViewOrient = "orient";
	private static final String configZipEncode = "zipencode";
	private static final String configPagingDirect = "pagingdirect";
	private static final String configDictEnabled = "dictenable";
	private static final String configDictFile = "dictfile";
	private static final String configFontFile = "fontfile";

	private int fontSize = SimpleTextView.defaultFontSize;
	private int color = SimpleTextView.defaultTextColor;
	private int bcolor = SimpleTextView.defaultBackgroundColor;
	private int ncolor = SimpleTextView.defaultNightTextColor;
	private int nbcolor = SimpleTextView.defaultNightBackgroundColor;
	private String currFile = null;
	private boolean hanStyle = true;
	private int viewOrient = Configuration.ORIENTATION_UNDEFINED;
	private String zipEncode = VFile.getDefaultEncode();
	private boolean colorBright = true;
	private boolean showStatus = true;
	private GestureDirect pagingDirect = Config.GestureDirect.up;
	private boolean dictEnabled = false;
	private String dictFile = null;
	private String fontFile = null;
	private ArrayList<ReadingInfo> rfl = new ArrayList<ReadingInfo>(MAX_RECENTLY_FILE_COUNT);

	public Config(Context context)
	{
		super(context, Reader.pathPrefix + "/" + configDB, null, DB_VERSION);
		if (GestureDirect.up.string == null) {
			for (GestureDirect gd : GestureDirect.values())
				gd.string = context.getString(gd.v());
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("create table " + CONFIG_TABLE_NAME + "(" + CONFIG_TABLE_COLS[0] + "  text primary key, " +
				   CONFIG_TABLE_COLS[1] + " text)");
		db.execSQL(
			"create table " + BOOK_INFO_TABLE_NAME + "(" + BOOK_INFO_TABLE_COLS[0] + " text primary key, " +
				BOOK_INFO_TABLE_COLS[1] + " integer, " + BOOK_INFO_TABLE_COLS[2] + " integer, " +
				BOOK_INFO_TABLE_COLS[3] + " integer, " + BOOK_INFO_TABLE_COLS[4] + " integer)");
		db.execSQL("create table " + BOOKMARKS_TABLE_NAME + "(" + BOOKMARKS_TABLE_COLS[0] + " integer, " +
				   BOOKMARKS_TABLE_COLS[1] + "  text, " + BOOKMARKS_TABLE_COLS[2] + " integer, " +
				   BOOKMARKS_TABLE_COLS[3] + " integer, " + BOOKMARKS_TABLE_COLS[4] + " integer)");

		// will this database be so big, that need to be indexed?
		db.execSQL(
			"create index bookmark_index on " + BOOKMARKS_TABLE_NAME + "(" + BOOKMARKS_TABLE_COLS[0] + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//no upgrade need, now
	}

	public void readConfig()
	{
		Map<String, String> config = new HashMap<String, String>();

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(CONFIG_TABLE_NAME, CONFIG_TABLE_COLS, null, null, null, null, null);
		while (cursor.moveToNext())
			config.put(cursor.getString(0), cursor.getString(1));
		cursor.close();

		currFile = getString(config, configCurrFile, null);
		fontSize = getInt(config, configFontSize, SimpleTextView.defaultFontSize);
		color = getInt(config, configColor, SimpleTextView.defaultTextColor);
		bcolor = getInt(config, configBColor, SimpleTextView.defaultBackgroundColor);
		ncolor = getInt(config, configNightColor, SimpleTextView.defaultNightTextColor);
		nbcolor = getInt(config, configNightBColor, SimpleTextView.defaultNightBackgroundColor);
		colorBright = getBoolean(config, configColorBright, true);
		hanStyle = getBoolean(config, configHanStyle, true);
		viewOrient = getInt(config, configViewOrient, Configuration.ORIENTATION_UNDEFINED);
		zipEncode = getString(config, configZipEncode, VFile.getDefaultEncode());
		dictEnabled = getBoolean(config, configDictEnabled, false);
		dictFile = getString(config, configDictFile, null);
		fontFile = getString(config, configFontFile, null);
		pagingDirect = GestureDirect.valueOf(getString(config, configPagingDirect, GestureDirect.up.s()));

		StringBuilder sql = new StringBuilder("select ");
		sql.append(BOOK_INFO_TABLE_COLS[0]);
		sql.append(',');
		sql.append(BOOK_INFO_TABLE_COLS[1]);
		sql.append(',');
		sql.append(BOOK_INFO_TABLE_COLS[2]);
		sql.append(',');
		sql.append(BOOK_INFO_TABLE_COLS[3]);
		sql.append(',');
		sql.append(BOOK_INFO_TABLE_COLS[4]);
		sql.append(", bi.");
		sql.append(BOOK_INFO_TABLE_COLS[5]);
		sql.append(" from ");
		sql.append(BOOK_INFO_TABLE_NAME);
		sql.append(" bi, ");
		sql.append(CONFIG_TABLE_NAME);
		sql.append(" cf where bi.");
		sql.append(BOOK_INFO_TABLE_COLS[0]);
		sql.append(" = cf.");
		sql.append(CONFIG_TABLE_COLS[1]);
		sql.append(" and cf.");
		sql.append(CONFIG_TABLE_COLS[0]);
		sql.append(" like '");
		sql.append(RECENTLY_FILE_PREFIX);
		sql.append("%' order by cf.");
		sql.append(CONFIG_TABLE_COLS[0]);

		rfl.clear();
		cursor = db.rawQuery(sql.toString(), null);
		while (cursor.moveToNext()) {
			ReadingInfo ri = new ReadingInfo();
			ri.name = cursor.getString(0);
			ri.chapter = cursor.getInt(1);
			ri.line = cursor.getInt(2);
			ri.offset = cursor.getInt(3);
			ri.percent = cursor.getInt(4);
			ri.id = cursor.getInt(5);
			rfl.add(ri);
		}
		cursor.close();
	}

	private String getString(Map<String, String> config, String name, @Nullable String defaultValue)
	{
		String v = config.get(name);
		return v != null ? v : defaultValue;
	}

	private int getInt(Map<String, String> config, String name, int defaultValue)
	{
		try {
			return new Integer(config.get(name));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private boolean getBoolean(Map<String, String> config, String name, boolean defaultValue)
	{
		try {
			return config.get(name).equals("true");
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public void saveConfig()
	{
		Map<String, String> config = new HashMap<String, String>();

		config.put(configFontSize, "" + fontSize);
		config.put(configColor, "" + color);
		config.put(configBColor, "" + bcolor);
		config.put(configNightColor, "" + ncolor);
		config.put(configNightBColor, "" + nbcolor);
		config.put(configCurrFile, currFile);
		config.put(configHanStyle, "" + hanStyle);
		config.put(configViewOrient, "" + viewOrient);
		config.put(configZipEncode, zipEncode);
		config.put(configColorBright, "" + colorBright);
		config.put(configPagingDirect, pagingDirect.s());
		config.put(configDictEnabled, "" + dictEnabled);
		config.put(configDictFile, "" + dictFile);
		config.put(configFontFile, "" + fontFile);

		for (int i = 0; i < rfl.size(); i++)
			config.put(RECENTLY_FILE_PREFIX + (i + 1), rfl.get(i).name);

		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("delete from " + CONFIG_TABLE_NAME);
		for (Map.Entry<String, String> e : config.entrySet())
			db.execSQL("insert into " + CONFIG_TABLE_NAME + " values (?, ?)",
				   new String[]{e.getKey(), e.getValue()});
	}

	public Config dup()
	{
		Config dup = new Config(null);
		dup.fontSize = fontSize;
		dup.color = color;
		dup.bcolor = bcolor;
		dup.ncolor = ncolor;
		dup.nbcolor = nbcolor;
		dup.colorBright = colorBright;
		dup.hanStyle = hanStyle;
		dup.showStatus = showStatus;
		dup.viewOrient = viewOrient;
		dup.pagingDirect = pagingDirect;
		dup.dictEnabled = dictEnabled;
		dup.dictFile = dictFile;
		dup.fontFile = fontFile;
		dup.zipEncode = zipEncode;
		return dup;
	}

	public void getback(Config dup)
	{
		fontSize = dup.fontSize;
		color = dup.color;
		bcolor = dup.bcolor;
		ncolor = dup.ncolor;
		nbcolor = dup.nbcolor;
		colorBright = dup.colorBright;
		hanStyle = dup.hanStyle;
		showStatus = dup.showStatus;
		viewOrient = dup.viewOrient;
		pagingDirect = dup.pagingDirect;
		dictEnabled = dup.dictEnabled;
		dictFile = dup.dictFile;
		fontFile = dup.fontFile;
		zipEncode = dup.zipEncode;
	}

	public String getCurrFile()
	{
		return currFile;
	}

	public void setReadingFile(String filename)
	{
		if (filename.equals(currFile))
			return;
		if (currFile != null)
			rfl.add(0, getReadingInfo(currFile));
		currFile = filename;
		int i;
		for (i = 1; i < rfl.size(); i++)
			if (rfl.get(i).name.equals(currFile)) {
				rfl.remove(i);
				return;
			}
		if (rfl.size() > MAX_RECENTLY_FILE_COUNT)
			rfl.remove(MAX_RECENTLY_FILE_COUNT);
	}

	public final List<ReadingInfo> getRecentFilesList()
	{
		return rfl;
	}

	public ReadingInfo getReadingInfo(String filename)
	{
		SQLiteDatabase db = getReadableDatabase();
		ReadingInfo ri = new ReadingInfo();
		ri.name = filename;
		ri.chapter = 0;
		ri.line = 0;
		ri.offset = 0;
		ri.percent = 0;
		ri.id = 0;

		Cursor cursor = db
			.query(BOOK_INFO_TABLE_NAME, BOOK_INFO_TABLE_COLS, "name = ?", new String[]{filename}, null,
			       null, null);
		if (cursor.moveToFirst()) {
			ri.chapter = cursor.getInt(1);
			ri.line = cursor.getInt(2);
			ri.offset = cursor.getInt(3);
			ri.percent = cursor.getInt(4);
			ri.id = cursor.getInt(5);
		}
		cursor.close();

		// we must insert new record and get back book id
		if (ri.id == 0)
			setReadingInfo(ri, true);
		return ri;
	}

	public void removeReadingInfo(String filename)
	{
		for (int i = 0; i < rfl.size(); i++)
			if (rfl.get(i).name.equals(filename))
				rfl.remove(i);

		SQLiteDatabase db = getWritableDatabase();
		db.delete(BOOK_INFO_TABLE_NAME, BOOK_INFO_TABLE_COLS[0] + " = ?", new String[]{filename});
	}

	public void setReadingInfo(ReadingInfo ri)
	{
		setReadingInfo(ri, false);
	}

	private void setReadingInfo(ReadingInfo ri, boolean insert)
	{
		if (ri == null)
			return;
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues(5);
		cv.put(BOOK_INFO_TABLE_COLS[0], ri.name);
		cv.put(BOOK_INFO_TABLE_COLS[1], ri.chapter);
		cv.put(BOOK_INFO_TABLE_COLS[2], ri.line);
		cv.put(BOOK_INFO_TABLE_COLS[3], ri.offset);
		cv.put(BOOK_INFO_TABLE_COLS[4], ri.percent);
		if (insert)
			ri.id = db.insert(BOOK_INFO_TABLE_NAME, null, cv);
		else
			db.update(BOOK_INFO_TABLE_NAME, cv, "rowid = ?", new String[]{Long.toString(ri.id)});
	}

	public boolean isHanStyle()
	{
		return hanStyle;
	}

	public void setHanStyle(boolean style)
	{
		hanStyle = style;
	}

	public void setFontSize(int size)
	{
		fontSize = size;
	}

	public int getFontSize()
	{
		return fontSize;
	}

	public void setColor(int aColor)
	{
		color = aColor;
	}

	public int getColor()
	{
		return color;
	}

	public void setBColor(int aColor)
	{
		bcolor = aColor;
	}

	public int getBColor()
	{
		return bcolor;
	}

	public int getNColor()
	{
		return ncolor;
	}

	public void setNColor(int ncolor)
	{
		this.ncolor = ncolor;
	}

	public int getNBColor()
	{
		return nbcolor;
	}

	public void setNBColor(int nbcolor)
	{
		this.nbcolor = nbcolor;
	}

	public boolean isViewLock()
	{
		return viewOrient != Configuration.ORIENTATION_UNDEFINED;
	}

	public void setViewOrient(int o)
	{
		viewOrient = o;
	}

	public void unsetViewOrient()
	{
		viewOrient = Configuration.ORIENTATION_UNDEFINED;
	}

	public int getViewOrient()
	{
		return viewOrient;
	}

	public String getZipEncode()
	{
		return zipEncode;
	}

	public void setZipEncode(String zipEncode)
	{
		this.zipEncode = zipEncode;
	}

	public boolean isColorBright()
	{
		return colorBright;
	}

	public void setColorBright(boolean colorBright)
	{
		this.colorBright = colorBright;
	}

	public GestureDirect getPagingDirect()
	{
		return pagingDirect;
	}

	public void setPagingDirect(GestureDirect pagingDirect)
	{
		this.pagingDirect = pagingDirect;
	}

	public boolean isDictEnabled()
	{
		return dictEnabled;
	}

	public void setDictEnabled(boolean dictEnabled)
	{
		this.dictEnabled = dictEnabled;
	}

	public String getDictFile()
	{
		return dictFile;
	}

	public void setDictFile(String dictFile)
	{
		this.dictFile = dictFile;
	}

	public String getFontFile()
	{
		return fontFile;
	}

	public void setFontFile(@Nullable String fontFile)
	{
		this.fontFile = fontFile;
	}

	public ArrayList<BookmarkManager.Bookmark> getBookmarkList(ReadingInfo ri)
	{
		if (ri == null)
			return null;

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db
			.query(BOOKMARKS_TABLE_NAME, BOOKMARKS_TABLE_COLS, BOOKMARKS_TABLE_COLS[0] + " = " + ri.id,
			       null, null, null,
			       BOOKMARKS_TABLE_COLS[2] + "," + BOOKMARKS_TABLE_COLS[3] + "," + BOOKMARKS_TABLE_COLS[4]);

		ArrayList<BookmarkManager.Bookmark> bml = new ArrayList<BookmarkManager.Bookmark>();

		while (cursor.moveToNext())
			bml.add(BookmarkManager.createBookmark(cursor.getString(1), cursor.getInt(2), cursor.getInt(3),
							       cursor.getInt(4), cursor.getInt(5), ri));
		cursor.close();

		return bml;
	}

	public void addBookmark(BookmarkManager.Bookmark bm)
	{
		assert bm.bookid != 0;

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues(5);
		cv.put(BOOKMARKS_TABLE_COLS[0], bm.bookid);
		cv.put(BOOKMARKS_TABLE_COLS[1], bm.desc);
		cv.put(BOOKMARKS_TABLE_COLS[2], bm.chapter);
		cv.put(BOOKMARKS_TABLE_COLS[3], bm.line);
		cv.put(BOOKMARKS_TABLE_COLS[4], bm.offset);

		db.insert(BOOKMARKS_TABLE_NAME, null, cv);
	}

	public void updateBookmark(BookmarkManager.Bookmark bm)
	{
		assert bm.bookid != 0;
		assert bm.getID() != 0;

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues(5);
		cv.put(BOOKMARKS_TABLE_COLS[0], bm.bookid);
		cv.put(BOOKMARKS_TABLE_COLS[1], bm.desc);
		cv.put(BOOKMARKS_TABLE_COLS[2], bm.chapter);
		cv.put(BOOKMARKS_TABLE_COLS[3], bm.line);
		cv.put(BOOKMARKS_TABLE_COLS[4], bm.offset);

		db.update(BOOKMARKS_TABLE_NAME, cv, "rowid = " + bm.getID(), null);
	}

	public void deleteBookmark(BookmarkManager.Bookmark bm)
	{
		assert bm.bookid != 0;
		assert bm.getID() != 0;

		SQLiteDatabase db = getWritableDatabase();
		db.delete(BOOKMARKS_TABLE_NAME, "rowid = " + bm.getID(), null);
	}

	public int getCurrentColor()
	{
		return (colorBright) ? color : ncolor;
	}

	public int getCurrentBColor()
	{
		return (colorBright) ? bcolor : nbcolor;
	}
}
