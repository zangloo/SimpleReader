package net.lzrj.SimpleReader.dict;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.util.HashMap;
import java.util.zip.DataFormatException;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 下午6:45
 */
public class SimpleReaderDict implements Dictionary
{
	public static final String EXTENSION = ".sqlite";
	private static final String DICT_INFO_TABLE_NAME = "info";
	private static final String[] DICT_INFO_QUERY_COLS = {"key", "value"};

	//public static final String DICT_VERSION_NAME = "version";
	private static final String DICT_HAS_WORD_NAME = "hasWord";
	private static final String DICT_MAX_KEY_LENGTH = "maxKeyLength";
	private static final String DICT_SELECT_SQL = "selectSQL";
	private static final String DICT_LIST_SQL = "listSQL";
	private static final String DICT_WEB_VIEW = "webView";

	private boolean dictHasWord = false;
	private boolean dictWebView = false;
	private int dictMaxWordLen = 1;
	//private String dictVersion = null;
	private String dictSelectSQL = null;
	private String dictListSQL = null;
	private SQLiteDatabase dictdb = null;
	private String name;
	private String filepath;

	public static DictionaryInfo info(File file) throws DataFormatException
	{
		SimpleReaderDict dict = new SimpleReaderDict();
		try {
			String filename = file.getName();
			dict.name = filename.substring(0, filename.length() - EXTENSION.length());
			dict.filepath = file.getAbsolutePath();
			dict.dictdb = SQLiteDatabase.openDatabase(dict.filepath, null, SQLiteDatabase.OPEN_READONLY |
				SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			Cursor cursor = dict.dictdb
				.query(DICT_INFO_TABLE_NAME, DICT_INFO_QUERY_COLS, null, null, null, null, null, null);
			if (!cursor.moveToFirst())
				throw new DataFormatException();

			HashMap<String, String> di = new HashMap<>();
			do {
				di.put(cursor.getString(0), cursor.getString(1));
			} while (cursor.moveToNext());
			dict.dictSelectSQL = di.get(DICT_SELECT_SQL);
			dict.dictListSQL = di.get(DICT_LIST_SQL);
			//dictVersion = di.get(DICT_VERSION_NAME);
			dict.dictHasWord = di.get(DICT_HAS_WORD_NAME).equals("true");
			if (dict.dictHasWord)
				dict.dictMaxWordLen = Integer.parseInt(di.get(DICT_MAX_KEY_LENGTH));
			else
				dict.dictMaxWordLen = 1;
			String s = di.get(DICT_WEB_VIEW);
			dict.dictWebView = (s != null && s.equals("true"));
			cursor.close();
			return dict;
		} catch (SQLiteException e) {
			throw new DataFormatException();
		}
	}

	@Override
	public boolean exists(String word)
	{
		Cursor cursor = dictdb.rawQuery(dictListSQL, new String[]{word});
		boolean exists = cursor.moveToNext();
		cursor.close();
		return exists;
	}

	@Override
	public String query(String word)
	{
		Cursor cursor = dictdb.rawQuery(dictSelectSQL, new String[]{word});
		if (!cursor.moveToNext())
			return null;
		String text = cursor.getString(1);
		cursor.close();
		return text;
	}

	@Override
	public int maxWordLength()
	{
		return dictMaxWordLen;
	}

	@Override
	public boolean webView()
	{
		return dictWebView;
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public String path()
	{
		return filepath;
	}

	@Override
	public Dictionary load()
	{
		return this;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
