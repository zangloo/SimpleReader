package net.lzrj.SimpleReader.popup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.R;
import net.lzrj.SimpleReader.book.Book;
import net.lzrj.SimpleReader.view.SimpleTextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 上午8:05
 */
public class BookmarkManager extends PopupList
{
	public static class Bookmark
	{
		public String desc;
		public int chapter;
		public int line, offset;
		public long bookid;
		private long id;

		public long getID()
		{
			return id;
		}
	}

	DialogInterface.OnClickListener addListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			bm.desc = et.getText().toString();
			config.addBookmark(bm);
			updateBookmarkList();
		}
	};
	DialogInterface.OnClickListener editListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			bm.desc = et.getText().toString();
			config.updateBookmark(bm);
			updateBookmarkList();
		}
	};
	DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			config.deleteBookmark(bm);
			updateBookmarkList();
		}
	};
	public static final int BOOKMARK_DESC_DEFAULT_LEN = 15;
	private static final String BOOKMARK_LIST_TITLE_DESC = "desc";
	private static final String BOOKMARK_LIST_TITLE_POS = "pos";

	private ArrayList<HashMap<String, Object>> bls;
	private ArrayList<BookmarkManager.Bookmark> bml;
	private SimpleAdapter sa;
	private Typeface tf = null;

	private Config config = null;
	private Config.ReadingInfo readingInfo = null;
	private Book book = null;

	private EditText et;
	private Bookmark bm;

	public BookmarkManager(Context context, Config conf, final AdapterView.OnItemClickListener onItemClickListener)
	{
		super(context);

		setTitle(context.getString(R.string.bookmark_title));
		config = conf;
		bls = new ArrayList<>();
		sa = new SimpleAdapter(context, bls, android.R.layout.two_line_list_item,
			new String[]{BOOKMARK_LIST_TITLE_DESC, BOOKMARK_LIST_TITLE_POS},
			new int[]{android.R.id.text1, android.R.id.text2})
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTextSize(20);
				tv.setTypeface(tf);
				tv.setTextColor(Color.BLACK);
				tv = (TextView) v.findViewById(android.R.id.text2);
				tv.setTextColor(Color.BLACK);
				return v;
			}
		};
		setAdapter(sa);
		setOnItemClickListener(onItemClickListener);
		setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				editDialog(bml.get(position));
				return true;
			}
		});

		setHeight(WindowManager.LayoutParams.FILL_PARENT);
	}

	public Bookmark getBookmark(int index)
	{
		return bml.get(index);
	}

	private void updateBookmarkList()
	{
		bls.clear();
		bml = config.getBookmarkList(readingInfo);
		if (bml != null)
			for (BookmarkManager.Bookmark b : bml) {
				HashMap<String, Object> map = new HashMap<>();
				map.put(BOOKMARK_LIST_TITLE_DESC, b.desc);
				if (book.chapterCount() > 1)
					map.put(BOOKMARK_LIST_TITLE_POS,
						book.chapterTitle(b.chapter) + "(" + b.line + " : " + b.offset +
							")");
				else
					map.put(BOOKMARK_LIST_TITLE_POS, "(" + b.line + " : " + b.offset + ")");
				bls.add(map);
			}
		sa.notifyDataSetChanged();
	}

	public void show(Config.ReadingInfo ri, Book b, Typeface typeface, int top)
	{
		readingInfo = ri;
		tf = typeface;
		book = b;
		updateBookmarkList();
		showAtLocation(getContentView(), Gravity.RIGHT | Gravity.CENTER, 0, top);
	}

	public void hide()
	{
		readingInfo = null;
		tf = null;
		book = null;
		dismiss();
	}

	public void addDialog(Bookmark bookmark)
	{
		bm = bookmark;
		if (bm.desc.length() > BOOKMARK_DESC_DEFAULT_LEN)
			bm.desc = bm.desc.substring(0, BOOKMARK_DESC_DEFAULT_LEN);
		Context context = getContentView().getContext();
		et = new EditText(context);
		et.setText(bm.desc);
		et.selectAll();

		new AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.bookmark_title) + bm.line + " , " + bm.offset)
			.setPositiveButton(R.string.button_ok_text, addListener)
			.setNegativeButton(R.string.button_cancel_text, null).setView(et).show();
	}

	public void editDialog(Bookmark bookmark)
	{
		bm = bookmark;

		Context context = getContentView().getContext();
		et = new EditText(context);
		et.setText(bm.desc);
		et.selectAll();

		new AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.bookmark_title) + bm.line + " , " + bm.offset)
			.setPositiveButton(R.string.button_ok_text, editListener)
			.setNeutralButton(R.string.button_bookmark_delete_title, deleteListener)
			.setNegativeButton(R.string.button_cancel_text, null).setView(et).show();
	}

	public static Bookmark createBookmark(SimpleTextView.TapTarget pi, Config.ReadingInfo ri)
	{
		return createBookmark(pi.str, ri.chapter, pi.line, pi.offset, 0, ri);
	}

	public static Bookmark createBookmark(String str, int c, int l, int o, int id, Config.ReadingInfo ri)
	{
		Bookmark bm = new Bookmark();
		bm.desc = str;
		bm.chapter = c;
		bm.line = l;
		bm.offset = o;
		bm.bookid = ri.getID();
		bm.id = id;
		return bm;
	}
}
