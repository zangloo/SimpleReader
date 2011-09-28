package zhang.lu.SimpleReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import zhang.lu.SimpleReader.Book.BookContent;
import zhang.lu.SimpleReader.View.SimpleTextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 上午8:05
 */
public class BookmarkManager extends PopupWindow
{
	public static class Bookmark
	{
		String desc;
		int chapter;
		int line, offset;
		long bookid;
		private long id;

		public long getID() {return id;}
	}

	public static interface OnBookmarkSelectListener
	{
		void onBookmarkSelect(Bookmark bookmark);
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
	private BookContent book = null;

	private EditText et;
	private Context reader;
	private View layout;
	private Bookmark bm;
	private OnBookmarkSelectListener bsl = null;

	public BookmarkManager(Context context, Config conf, OnBookmarkSelectListener onBookmarkSelectListener)
	{
		super(context);
		reader = context;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.bookmark, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		bsl = onBookmarkSelectListener;
		config = conf;
		bls = new ArrayList<HashMap<String, Object>>();
		ListView bl = (ListView) layout.findViewById(R.id.bookmark_list);
		sa = new SimpleAdapter(reader, bls, android.R.layout.two_line_list_item,
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
		bl.setAdapter(sa);
		bl.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				bsl.onBookmarkSelect(bml.get(position));
			}
		});
		bl.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				editDialog(bml.get(position));
				return true;
			}
		});
	}

	private void updateBookmarkList()
	{
		bls.clear();
		bml = config.getBookmarkList(readingInfo);
		if (bml != null)
			for (BookmarkManager.Bookmark b : bml) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put(BOOKMARK_LIST_TITLE_DESC, b.desc);
				if (book.getChapterCount() > 1)
					map.put(BOOKMARK_LIST_TITLE_POS,
						book.getChapterTitle(b.chapter) + "(" + b.line + " : " + b.offset +
							")");
				else
					map.put(BOOKMARK_LIST_TITLE_POS, "(" + b.line + " : " + b.offset + ")");
				bls.add(map);
			}
		sa.notifyDataSetChanged();
	}

	public void show(Config.ReadingInfo ri, BookContent bookContent, Typeface typeface, int top, int width, int height)
	{
		readingInfo = ri;
		tf = typeface;
		book = bookContent;
		setWidth(width);
		setHeight(height);
		updateBookmarkList();
		showAtLocation(layout, Gravity.LEFT | Gravity.CENTER, 0, top);
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
		assert bm.id == 0;

		if (bm.desc.length() > BOOKMARK_DESC_DEFAULT_LEN)
			bm.desc = bm.desc.substring(0, BOOKMARK_DESC_DEFAULT_LEN);
		et = new EditText(reader);
		et.setText(bm.desc);
		et.selectAll();

		new AlertDialog.Builder(reader)
			.setTitle(reader.getString(R.string.bookmark_title) + bm.line + " , " + bm.offset)
			.setPositiveButton(R.string.button_ok_text, addListener)
			.setNegativeButton(R.string.button_cancel_text, null).setView(et).show();
	}

	public void editDialog(Bookmark bookmark)
	{
		bm = bookmark;
		assert bm.id != 0;

		et = new EditText(reader);
		et.setText(bm.desc);
		et.selectAll();

		new AlertDialog.Builder(reader)
			.setTitle(reader.getString(R.string.bookmark_title) + bm.line + " , " + bm.offset)
			.setPositiveButton(R.string.button_ok_text, editListener)
			.setNeutralButton(R.string.button_bookmark_delete_title, deleteListener)
			.setNegativeButton(R.string.button_cancel_text, null).setView(et).show();
	}

	public static Bookmark createBookmark(SimpleTextView.FingerPosInfo pi, Config.ReadingInfo ri)
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
