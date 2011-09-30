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
		void onMenuSelect(menu id);
	}

	public static enum menu
	{
		bookmark, dict, menu, exit
	}

	// same order with enum menu
	private static final int[] menuTitleIDS = new int[]{R.string.menu_bookmark, R.string.menu_dict, R.string.menu_menu, R.string.menu_exit};
	private static final int POPUP_WINDOW_BOARD_SIZE = 4 * 2;

	private View layout;
	private ArrayList<String> mls = new ArrayList<String>();
	private ArrayAdapter<String> aa;
	private ListView ml;
	private HashMap<Integer, String> menuInfo = new HashMap<Integer, String>();

	public PopupMenu(Context context, final OnMenuSelectListener onMenuSelectListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.popuplist, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		for (int id : menuTitleIDS)
			menuInfo.put(id, context.getString(id));
		ml = (ListView) layout.findViewById(R.id.popup_list);
		aa = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mls)
		{
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
				onMenuSelectListener.onMenuSelect(menu.values()[position]);
			}
		});
	}

	public void show(String title, Typeface typeface, int width, int x, int y, boolean showDict)
	{
		TextView tv = (TextView) layout.findViewById(R.id.popup_list_label);
		tv.setText(title);
		tv.setTypeface(typeface);

		mls.clear();
		if (title != null) {
			mls.add(menuInfo.get(R.string.menu_bookmark));
			if (showDict)
				mls.add(menuInfo.get(R.string.menu_dict));
		}
		mls.add(menuInfo.get(R.string.menu_menu));
		mls.add(menuInfo.get(R.string.menu_exit));
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
