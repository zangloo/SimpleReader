package zhang.lu.SimpleReader.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import zhang.lu.SimpleReader.R;
import zhang.lu.SimpleReader.Util;
import zhang.lu.SimpleReader.view.SimpleTextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 下午6:45
 */
public class DictManager
{
	private static final Pattern DictTextATag = Pattern.compile("<a[ ]+href=\"([^\"]+)\">");

	static class DictData
	{
		String key;
		String part;
		String desc;
	}

	public static final String DICT_INFO_TABLE_NAME = "info";
	public static final String[] DICT_INFO_QUERY_COLS = {"key", "value"};

	//public static final String DICT_VERSION_NAME = "version";
	public static final String DICT_HAS_WORD_NAME = "hasWord";
	public static final String DICT_MAX_KEY_LENGTH = "maxKeyLength";
	public static final String DICT_SELECT_SQL = "selectSQL";
	public static final String DICT_LIST_SQL = "listSQL";
	public static final String DICT_WEB_VIEW = "webView";

	private Context context;
	private AlertDialog ad;
	private boolean dictHasWord = false;
	private boolean dictWebView = false;
	private int dictMaxWordLen = 1;
	//private String dictVersion = null;
	private String dictSelectSQL = null;
	private String dictListSQL = null;
	private SQLiteDatabase dictdb = null;
	private String[] dictList;
	private String dictName;

	private DialogInterface.OnClickListener dictListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			if ((dictList != null) && (dictList.length > which))
				showDictDlg(dictList[which]);
		}
	};

	public DictManager(Context context)
	{
		this.context = context;
	}

	public int getDictMaxWordLen()
	{
		return dictMaxWordLen;
	}

	public void loadDict(String filename, String dname)
	{
		if (dictdb != null)
			unloadDict();
		try {
			dictName = dname;
			dictdb = SQLiteDatabase.openDatabase(filename, null, SQLiteDatabase.OPEN_READONLY |
				SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			Cursor cursor = dictdb
				.query(DICT_INFO_TABLE_NAME, DICT_INFO_QUERY_COLS, null, null, null, null, null, null);
			if (!cursor.moveToFirst()) {
				dictdb.close();
				dictdb = null;
				return;
			}
			HashMap<String, String> di = new HashMap<String, String>();
			do {
				di.put(cursor.getString(0), cursor.getString(1));
			} while (cursor.moveToNext());
			dictSelectSQL = di.get(DICT_SELECT_SQL);
			dictListSQL = di.get(DICT_LIST_SQL);
			//dictVersion = di.get(DICT_VERSION_NAME);
			dictHasWord = di.get(DICT_HAS_WORD_NAME).equals("true");
			if (dictHasWord)
				dictMaxWordLen = new Integer(di.get(DICT_MAX_KEY_LENGTH));
			else
				dictMaxWordLen = 1;
			String s = di.get(DICT_WEB_VIEW);
			dictWebView = (s != null && s.equals("true"));
			cursor.close();
		} catch (SQLiteException e) {
			dictdb = null;
			Util.errorMsg(context, e.getMessage());
		}
	}

	public void unloadDict()
	{
		if (dictdb == null)
			return;
		dictdb.close();
		dictdb = null;
	}

	private String[] getDictList(String str)
	{
		if (dictdb == null)
			return null;
		if (str == null)
			return null;
		int len = str.length();
		if (len == 0)
			return null;
		try {
			String[] para = new String[1];
			ArrayList<String> list = new ArrayList<String>();
			if (len > dictMaxWordLen)
				len = dictMaxWordLen;
			for (int i = 0; i < len; i++) {
				para[0] = str.substring(0, i + 1);
				Cursor cursor = dictdb.rawQuery(dictListSQL, para);
				if (cursor.moveToNext())
					list.add(para[0]);
				cursor.close();
			}
			if (list.size() > 0)
				return list.toArray(new String[list.size()]);
			else
				return null;
		} catch (SQLiteException e) {
			Util.errorMsg(context, e.getMessage());
			return null;
		}
	}

	private DictData getDictData(String str)
	{
		if (dictdb == null)
			return null;
		if (str == null)
			return null;
		try {
			Cursor cursor = dictdb.rawQuery(dictSelectSQL, new String[]{str});
			if (!cursor.moveToNext())
				return null;
			DictData di = new DictData();
			di.key = str;
			di.part = cursor.getString(0);
			di.desc = cursor.getString(1);
			cursor.close();
			return di;
		} catch (SQLiteException e) {
			Util.errorMsg(context, e.getMessage());
			return null;
		}
	}

	public void showDict(SimpleTextView.FingerPosInfo fingerPosInfo)
	{
		if (dictHasWord) {
			dictList = getDictList(fingerPosInfo.str);
			if (dictList != null)
				if (dictList.length > 1)
					dictListDlg(dictList);
				else
					showDictDlg(dictList[0]);
		} else
			showDictDlg(fingerPosInfo.str.substring(0, 1));
	}

	private void dictListDlg(String[] list)
	{
		new AlertDialog.Builder(context).setTitle(R.string.dict_list_title)
			.setNegativeButton(R.string.button_cancel_text, null)
			.setItems(list, dictListener).show();
	}

	private void showDictDlg(String key)
	{
		DictData dd = getDictData(key);
		if (dd == null)
			return;
		if (dictWebView)
			dictDlgWeb(dd);
		else
			dictDlgPlain(dd);
	}

	private void dictDlgPlain(DictData dd)
	{
		new AlertDialog.Builder(context).setTitle(dictName + ": " + dd.key).setMessage(dd.part + "\n" + dd.desc)
			.setPositiveButton(R.string.button_ok_text, null).show();
	}

	private void buildWebViewText(DictData dd, WebView wv)
	{
		String data;
		if (dd.part.length() == 0)
			data = dd.desc;
		else
			data = dd.part + "<br/>" + dd.desc;

		String base;
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			Matcher matcher = DictTextATag.matcher(data);
			StringBuffer resultString = new StringBuffer();
			while (matcher.find())
				matcher.appendReplacement(resultString, "<a href=\"data://" + matcher.group(1) + "\">");

			matcher.appendTail(resultString);

			data = resultString.toString();
			base = "data://";
		} else
			base = null;

		wv.loadDataWithBaseURL(base, data, "text/html", "utf-8", null);
	}

	private void dictDlgWeb(DictData dd)
	{
		WebView wv = new WebView(context);
		wv.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				DictData dd = null;
				try {
					if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1)
						url = URLDecoder.decode(url, "UTF-8").substring(7);
					dd = getDictData(url);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if (dd != null) {
					buildWebViewText(dd, view);
					view.scrollTo(0, 0);
					ad.setTitle(dictName + ": " + dd.key);
				}
				return true;
			}
		});
		buildWebViewText(dd, wv);
		ad = new AlertDialog.Builder(context).setTitle(dictName + ": " + dd.key).setView(wv)
			.setPositiveButton(R.string.button_ok_text, null).show();
	}
}
