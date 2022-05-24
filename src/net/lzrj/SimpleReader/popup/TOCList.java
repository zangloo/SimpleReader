package net.lzrj.SimpleReader.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import net.lzrj.SimpleReader.R;
import net.lzrj.SimpleReader.book.TOCRecord;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 下午1:28
 */
public class TOCList extends PopupList
{
	private Typeface tf = null;
	private ArrayAdapter<String> tocAdapter;
	private int tocIndexOfChapter;

	public TOCList(Context context, final AdapterView.OnItemClickListener onItemClickListener)
	{
		super(context);

		setTitle(context.getString(R.string.menu_chapter));
		tocAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTypeface(tf);
				if (position == tocIndexOfChapter)
					tv.setTextColor(Color.rgb(0, 150, 150));
				else
					tv.setTextColor(Color.BLACK);
				return v;
			}
		};
		setAdapter(tocAdapter);
		setOnItemClickListener(onItemClickListener);

		setHeight(WindowManager.LayoutParams.FILL_PARENT);
	}

	public void show(ArrayList<TOCRecord> cs, int index, Typeface typeface, int top)
	{
		tocAdapter.clear();
		for (int i = 0; i < cs.size(); i++) {
			TOCRecord c = cs.get(i);
			String text;
			if (c.level() == 0) {
				text = "●" + c.title();
			} else {
				text = c.title();
			}
			if (i == index)
				text = "> " + text;
			tocAdapter.add(text);
		}
		tocAdapter.notifyDataSetChanged();

		tf = typeface;
		showAtLocation(getContentView(), Gravity.LEFT | Gravity.CENTER, 0, top);
		setSelection(tocIndexOfChapter = index);
	}

	public void hide()
	{
		tf = null;
		dismiss();
	}
}
