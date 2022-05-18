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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 下午1:28
 */
public class PopupMenu extends PopupList
{
	private static class PopupMenuItem
	{
		private int id;

		public PopupMenuItem(int id)
		{
			this.id = id;
		}

		@Override
		public String toString()
		{
			return menuInfo.get(id);
		}
	}

	private static HashMap<Integer, String> menuInfo;

	private ArrayAdapter<PopupMenuItem> aa;

	public PopupMenu(Context context, HashMap<Integer, String> mi, final AdapterView.OnItemClickListener onItemClickListener)
	{
		super(context);

		menuInfo = mi;
		aa = new ArrayAdapter<PopupMenuItem>(context, android.R.layout.simple_list_item_1)
		{
			@Override
			public long getItemId(int position)
			{
				return getItem(position).id;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTextColor(Color.BLACK);
				return v;
			}
		};
		setAdapter(aa);
		setOnItemClickListener(onItemClickListener);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
	}

	public void show(String title, Typeface typeface, int width, int x, int y, ArrayList<Integer> items)
	{
		setTitle(title);
		setTitleTypeface(typeface);

		aa.clear();
		for (int i : items)
			aa.add(new PopupMenuItem(i));
		aa.notifyDataSetChanged();

		setWidth(width);
		showAtLocation(getContentView(), Gravity.TOP | Gravity.LEFT, x, y);
	}

	public void hide()
	{
		dismiss();
	}
}
