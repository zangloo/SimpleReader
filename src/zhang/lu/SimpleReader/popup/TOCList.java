package zhang.lu.SimpleReader.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import zhang.lu.SimpleReader.R;
import zhang.lu.SimpleReader.book.TOCRecord;

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
	private ArrayAdapter<String> aa;
	private int chapter;

	public TOCList(Context context, final AdapterView.OnItemClickListener onItemClickListener)
	{
		super(context);

		setTitle(context.getString(R.string.menu_chapter));
		aa = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setTypeface(tf);
				if (position == chapter)
					tv.setTextColor(Color.rgb(0, 250, 250));
				else
					tv.setTextColor(Color.WHITE);
				return v;
			}
		};
		setAdapter(aa);
		setOnItemClickListener(onItemClickListener);
	}

	public void show(ArrayList<TOCRecord> cs, int index, Typeface typeface, int top, int width, int height)
	{
		aa.clear();
		for (TOCRecord c : cs)
			if (c.level() == 0)
				aa.add("●" + c.title());
			else
				aa.add(c.title());

		aa.notifyDataSetChanged();

		tf = typeface;
		setWidth(width);
		setHeight(height);
		showAtLocation(getContentView(), Gravity.LEFT | Gravity.CENTER, 0, top);
		setSelection(chapter = index);
	}

	public void hide()
	{
		tf = null;
		dismiss();
	}
}