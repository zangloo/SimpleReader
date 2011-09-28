package zhang.lu.SimpleReader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

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
	private ArrayList<String> cls = new ArrayList<String>();
	private ArrayAdapter<String> aa;
	private View layout;
	private ListView cl;
	private int chapter;

	public ChapterManager(Context context, OnChapterSelectListener onChapterSelectListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.chapter, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		csl = onChapterSelectListener;
		cl = (ListView) layout.findViewById(R.id.chapter_list);
		aa = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, cls)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTypeface(tf);
				tv.setTextColor(Color.BLACK);
				if (position == chapter)
					tv.setBackgroundResource(android.R.color.darker_gray);
				else
					tv.setBackgroundResource(android.R.color.background_light);
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

	public void show(ArrayList<String> ls, int index, Typeface typeface, int top, int width, int height)
	{
		cls.clear();
		cls.addAll(ls);
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
