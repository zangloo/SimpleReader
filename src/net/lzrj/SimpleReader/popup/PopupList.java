package net.lzrj.SimpleReader.popup;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import net.lzrj.SimpleReader.R;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-10-8
 * Time: 下午6:27
 */
public class PopupList extends PopupWindow
{
	private ListView lv;
	private TextView tv;
	static private int mw = 0;

	public PopupList(Context context)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.popuplist, null, true);

		setContentView(layout);
		setFocusable(true);
		tv = (TextView) layout.findViewById(R.id.popup_list_label);
		lv = (ListView) layout.findViewById(R.id.popup_list);
	}

	public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener)
	{
		lv.setOnItemClickListener(onItemClickListener);
	}

	public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemLongClickListener)
	{
		lv.setOnItemLongClickListener(onItemLongClickListener);
	}

	public void setAdapter(ListAdapter adapter)
	{
		lv.setAdapter(adapter);
	}

	public void setTitle(String title)
	{
		tv.setText(title);
	}

	public void setSelection(int index)
	{
		lv.setSelection(index);
	}

	public void setTitleTypeface(Typeface tf)
	{
		tv.setTypeface(tf);
	}

	static public void setMaxWidth(int width)
	{
		mw = width;
	}

	@Override
	public void update(int width, int height)
	{
		if (mw != 0)
			width = Math.min(mw, width);
		super.update(width, height);
	}
}
