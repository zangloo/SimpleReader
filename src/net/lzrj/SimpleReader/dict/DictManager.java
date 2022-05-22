package net.lzrj.SimpleReader.dict;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteException;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import net.lzrj.SimpleReader.R;
import net.lzrj.SimpleReader.Util;
import net.lzrj.SimpleReader.view.SimpleTextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 下午6:45
 */
public class DictManager
{
	private static final Pattern DictTextATag = Pattern.compile("<a[ ]+href=\"([^\"]+)\">");

	public static List<DictionaryInfo> detectDictionaries(String dictRoot)
	{
		List<DictionaryInfo> list = new ArrayList<>();
		collectDict(dictRoot, list);
		return list;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void collectDict(String path, final List<DictionaryInfo> list)
	{
		File file = new File(path);
		if (file.isDirectory())
			file.list(new FilenameFilter()
			{
				public boolean accept(File file, String s)
				{
					collectDict(file.getAbsolutePath() + "/" + s, list);
					return false;
				}
			});
		String filename = file.getName().toLowerCase();
		try {
			if (filename.endsWith(StarDict.EXTENSION))
				list.add(StarDict.info(file));
			else if (filename.endsWith(SimpleReaderDict.EXTENSION))
				list.add(SimpleReaderDict.info(file));
		} catch (IOException | DataFormatException ignored) {
		}
	}

	static class DictData
	{
		final String word;
		final String definition;

		DictData(String word, String definition)
		{
			this.word = word;
			this.definition = definition;
		}
	}

	private final Context context;
	private AlertDialog ad;
	private Dictionary dictionary = null;
	private String[] dictList;

	private final DialogInterface.OnClickListener dictListener = new DialogInterface.OnClickListener()
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
		return dictionary.maxWordLength();
	}

	public void loadDictionary(String filepath)
	{
		try {
			filepath = filepath.toLowerCase();
			File file = new File(filepath);
			DictionaryInfo info;
			if (filepath.endsWith(StarDict.EXTENSION))
				info = StarDict.info(file);
			else if (filepath.endsWith(SimpleReaderDict.EXTENSION))
				info = SimpleReaderDict.info(file);
			else {
				return;
			}
			dictionary = info.load();
		} catch (IOException | DataFormatException e) {
			dictionary = null;
			Util.errorMsg(context, e.getMessage());
		}
	}

	public boolean loaded()
	{
		return dictionary != null;
	}

	public void unloadDict()
	{
		dictionary = null;
	}

	private String[] getDictList(String str)
	{
		if (dictionary == null)
			return null;
		if (str == null)
			return null;
		int len = str.length();
		if (len == 0)
			return null;
		try {
			ArrayList<String> list = new ArrayList<>();
			if (len > dictionary.maxWordLength())
				len = dictionary.maxWordLength();
			for (int i = 0; i < len; i++) {
				String pattern = str.substring(0, i + 1);
				if (dictionary.exists(pattern))
					list.add(pattern);
			}
			if (list.size() > 0)
				return list.toArray(new String[0]);
			else
				return null;
		} catch (SQLiteException e) {
			Util.errorMsg(context, e.getMessage());
			return null;
		}
	}

	private DictData getDictData(String word)
	{
		if (dictionary == null)
			return null;
		if (word == null || word.length() == 0)
			return null;
		try {
			String text = dictionary.query(word);
			return new DictData(word, text);
		} catch (SQLiteException e) {
			Util.errorMsg(context, e.getMessage());
			return null;
		}
	}

	public void showDict(SimpleTextView.TapTarget tapTarget)
	{
		dictList = getDictList(tapTarget.str);
		if (dictList != null)
			if (dictList.length > 1)
				dictListDlg(dictList);
			else
				showDictDlg(dictList[0]);
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
		if (dictionary.webView())
			dictDlgWeb(dd);
		else
			dictDlgPlain(dd);
	}

	private void dictDlgPlain(DictData dd)
	{
		new AlertDialog.Builder(context).setTitle(dictionary.name() + ": " + dd.word).setMessage(dd.definition)
			.setPositiveButton(R.string.button_ok_text, null).show();
	}


	private void buildWebViewText(DictData dd, WebView wv)
	{
		String data = dd.definition;

		String base;
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
					if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
						url = URLDecoder.decode(url, "UTF-8").substring(7);
					dd = getDictData(url);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if (dd != null) {
					buildWebViewText(dd, view);
					view.scrollTo(0, 0);
					ad.setTitle(dictionary.name() + ": " + dd.word);
				}
				return true;
			}
		});
		buildWebViewText(dd, wv);
		ad = new AlertDialog.Builder(context).setTitle(dictionary.name() + ": " + dd.word).setView(wv)
			.setPositiveButton(R.string.button_ok_text, null).show();
	}
}
