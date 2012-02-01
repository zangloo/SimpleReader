package zhang.lu.SimpleReader.popup;

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
import zhang.lu.SimpleReader.R;

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

	// same order with enum menu
	private static final int[] menuTitleIDS = new int[]{R.string.menu_bookmark, R.string.menu_dict};
	private static HashMap<Integer, String> menuInfo = new HashMap<Integer, String>();

	private ArrayAdapter<PopupMenuItem> aa;

	public PopupMenu(Context context, final AdapterView.OnItemClickListener onItemClickListener)
	{
		super(context);

		menuInfo.clear();
		for (int id : menuTitleIDS)
			menuInfo.put(id, context.getString(id));
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
				tv.setTextColor(Color.WHITE);
				return v;
			}
		};
		setAdapter(aa);
		setOnItemClickListener(onItemClickListener);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
	}

	public void show(String title, Typeface typeface, int width, int x, int y, boolean showDict)
	{
		if (title == null)
			setTitle(getContentView().getContext().getString(R.string.no_text_selected));
		else
			setTitle(title);
		setTitleTypeface(typeface);

		aa.clear();
		if (title != null) {
			aa.add(new PopupMenuItem(R.string.menu_bookmark));
			if (showDict)
				aa.add(new PopupMenuItem(R.string.menu_dict));
		}
		aa.notifyDataSetChanged();

		setWidth(width);
		showAtLocation(getContentView(), Gravity.TOP | Gravity.LEFT, x, y);
	}

	public void hide()
	{
		dismiss();
	}
}
