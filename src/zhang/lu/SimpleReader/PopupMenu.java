package zhang.lu.SimpleReader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 下午1:28
 */
public class PopupMenu extends PopupWindow
{
	public static interface OnMenuSelectListener
	{
		void onMenuSelect(int id);
	}

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
	private static final int[] menuTitleIDS = new int[]{R.string.menu_bookmark, R.string.menu_dict, R.string.menu_menu, R.string.menu_exit};
	private static final int POPUP_WINDOW_BOARD_SIZE = 4 * 2;

	private static HashMap<Integer, String> menuInfo = new HashMap<Integer, String>();

	private View layout;
	private ArrayAdapter<PopupMenuItem> aa;
	private ListView ml;

	public PopupMenu(Context context, final OnMenuSelectListener onMenuSelectListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.popuplist, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		menuInfo.clear();
		for (int id : menuTitleIDS)
			menuInfo.put(id, context.getString(id));
		ml = (ListView) layout.findViewById(R.id.popup_list);
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
		ml.setAdapter(aa);
		ml.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				onMenuSelectListener.onMenuSelect((int) id);
			}
		});
	}

	public void show(String title, Typeface typeface, int width, int x, int y, boolean showDict)
	{
		TextView tv = (TextView) layout.findViewById(R.id.popup_list_label);
		tv.setText(title);
		tv.setTypeface(typeface);

		aa.clear();
		if (title != null) {
			aa.add(new PopupMenuItem(R.string.menu_bookmark));
			if (showDict)
				aa.add(new PopupMenuItem(R.string.menu_dict));
		}
		aa.add(new PopupMenuItem(R.string.menu_menu));
		aa.add(new PopupMenuItem(R.string.menu_exit));
		aa.notifyDataSetChanged();

		int h = 0;
		for (int i = 0; i < aa.getCount(); i++) {
			View li = aa.getView(i, null, ml);
			li.measure(0, 0);
			h += li.getMeasuredHeight() + ml.getDividerHeight();
		}
		tv.measure(width + View.MeasureSpec.EXACTLY, View.MeasureSpec.UNSPECIFIED);
		setHeight(h + tv.getMeasuredHeight() + 2 + POPUP_WINDOW_BOARD_SIZE);
		setWidth(width + POPUP_WINDOW_BOARD_SIZE);
		showAtLocation(layout, Gravity.TOP | Gravity.LEFT, x, y);
	}

	public void hide()
	{
		dismiss();
	}
}
