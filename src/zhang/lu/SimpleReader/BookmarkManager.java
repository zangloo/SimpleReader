package zhang.lu.SimpleReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import zhang.lu.SimpleReader.View.SimpleTextView;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-4
 * Time: 上午8:05
 */
public class BookmarkManager
{
	public static class Bookmark
	{
		String desc;
		int line, offset;
		int bookid;
		private int id;

		public int getID() {return id;}
	}

	public static interface OnBookmarkEditListener
	{
		void onBookmarkAdd(Bookmark bookmark);

		void onBookmarkDelete(Bookmark bookmark);

		void onBookmarkEdit(Bookmark bookmark);
	}

	EditText et;
	Context reader;
	Bookmark bm;
	OnBookmarkEditListener onBookmarkEditListener = null;
	DialogInterface.OnClickListener addListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			bm.desc = et.getText().toString();
			onBookmarkEditListener.onBookmarkAdd(bm);
		}
	};
	DialogInterface.OnClickListener editListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			bm.desc = et.getText().toString();
			onBookmarkEditListener.onBookmarkEdit(bm);
		}
	};
	DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			onBookmarkEditListener.onBookmarkDelete(bm);
		}
	};

	protected BookmarkManager(Context context, OnBookmarkEditListener l1)
	{
		reader = context;

		onBookmarkEditListener = l1;
	}

	public void addDialog(Bookmark bookmark)
	{
		bm = bookmark;
		assert bm.id == 0;

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
		return createBookmark(pi.str, pi.line, pi.offset, 0, ri);
	}

	public static Bookmark createBookmark(String str, int l, int o, int id, Config.ReadingInfo ri)
	{
		Bookmark bm = new Bookmark();
		bm.desc = str;
		bm.line = l;
		bm.offset = o;
		bm.bookid = ri.getID();
		bm.id = id;
		return bm;
	}
}
