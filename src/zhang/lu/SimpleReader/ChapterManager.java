package zhang.lu.SimpleReader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import zhang.lu.SimpleReader.Book.BookContent;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 下午1:28
 */
public class ChapterManager extends PopupWindow
{
	public static interface OnChapterSelectListener
	{
		void onChapterSelect(int chapter);
	}

	private OnChapterSelectListener csl;
	private Typeface tf = null;
	private ArrayAdapter<String> aa;
	private View layout;
	private ListView cl;
	private int chapter;

	public ChapterManager(Context context, OnChapterSelectListener onChapterSelectListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.popuplist, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		TextView tv = (TextView) layout.findViewById(R.id.popup_list_label);
		tv.setText(context.getString(R.string.menu_chapter));
		csl = onChapterSelectListener;
		cl = (ListView) layout.findViewById(R.id.popup_list);
		aa = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTypeface(tf);
				if (position == chapter)
					tv.setTextColor(Color.RED);
				else
					tv.setTextColor(Color.BLACK);
				return v;
			}
		};
		cl.setAdapter(aa);
		cl.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				csl.onChapterSelect(position);
			}
		});
	}

	public void show(ArrayList<BookContent.ChapterInfo> cs, int index, Typeface typeface, int top, int width, int height)
	{
		aa.clear();
		for (BookContent.ChapterInfo c : cs)
			aa.add(c.title());
		aa.notifyDataSetChanged();

		tf = typeface;
		setWidth(width);
		setHeight(height);
		showAtLocation(layout, Gravity.LEFT | Gravity.CENTER, 0, top);
		cl.setSelection(chapter = index);
	}

	public void hide()
	{
		tf = null;
		dismiss();
	}
}
