package zhang.lu.SimpleReader;

import android.app.Dialog;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.*;
import org.jetbrains.annotations.Nullable;
import zhang.lu.SimpleReader.Book.VFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Zhanglu
 * Date: 10-12-4
 * Time: 下午8:58
 */
public class FileDialog extends Dialog implements AdapterView.OnItemClickListener
{
	public static interface OnFilePickedListener
	{
		void onFilePicked(String filename);
	}

	private static final String upDir = "..";

	private static final String[] LIST_HEAD_NAMES = new String[]{"icon", "name", "info"};
	private static final int[] LIST_HEAD_ID = new int[]{R.id.file_icon, R.id.file_name, R.id.file_info};

	private List<HashMap<String, Object>> fns;
	private List<HashMap<String, Object>> rfns;
	private SimpleAdapter saf;
	private SimpleAdapter sarf;
	private ListView[] lv = new ListView[2];
	private ViewPager vp;
	private RadioGroup rg;
	private String pwd;

	private OnFilePickedListener fpl;

	public FileDialog(Context context)
	{
		super(context);
	}

	public void init(OnFilePickedListener listener)
	{
		setContentView(R.layout.filedlg);
		setTitle(getContext().getString(R.string.dialog_file_title));

		fpl = listener;
		// setup recently files list
		lv[1] = new ListView(getContext());
		rfns = new ArrayList<HashMap<String, Object>>();
		sarf = new SimpleAdapter(getContext(), rfns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[1].setAdapter(sarf);
		lv[1].setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				filePicked((String) rfns.get(position).get("name"));
			}
		});

		// setup file list
		fns = new ArrayList<HashMap<String, Object>>();
		saf = new SimpleAdapter(getContext(), fns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[0] = new ListView(getContext());
		lv[0].setAdapter(saf);
		lv[0].setOnItemClickListener(this);

		vp = (ViewPager) findViewById(R.id.file_list_pager);
		vp.setOnPageChangeListener(new android.support.v4.view.ViewPager.OnPageChangeListener()
		{
			public void onPageScrolled(int i, float v, int i1)
			{
			}

			public void onPageSelected(int i)
			{
				switch (i) {
					case 0:
						rg.check(R.id.button_file_list);
						break;
					case 1:
						rg.check(R.id.button_recently_list);
						break;
				}
			}

			public void onPageScrollStateChanged(int i)
			{
			}
		});
		vp.setAdapter(new PagerAdapter()
		{
			@Override
			public int getCount()
			{
				return 2;
			}

			@Override
			public void startUpdate(View view)
			{
			}

			@Override
			public Object instantiateItem(View view, int i)
			{
				((ViewPager) view).addView(lv[i], i);

				return lv[i];
			}

			@Override
			public void destroyItem(View view, int i, Object o)
			{
				((ViewPager) view).removeView((View) o);
			}

			@Override
			public void finishUpdate(View view)
			{
			}

			@Override
			public boolean isViewFromObject(View view, Object o)
			{
				return view == o;
			}

			@Override
			public Parcelable saveState()
			{
				return null;
			}

			@Override
			public void restoreState(Parcelable parcelable, ClassLoader classLoader)
			{
			}
		});

		Button btn = (Button) findViewById(R.id.button_cancel);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dismiss();
			}
		});

		rg = (RadioGroup) findViewById(R.id.file_list_radio_group);
		rg.check(R.id.button_file_list);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				switch (checkedId) {
					case R.id.button_file_list:
						vp.setCurrentItem(0);
						break;
					case R.id.button_recently_list:
						vp.setCurrentItem(1);
						break;
				}
			}
		});
	}

	public void update(String path, List<Config.ReadingInfo> recentFileList)
	{
		String fn = null;
		//if path is root, it should be "" not "/"
		if (path == null)
			pwd = "";
		else {
			int p = path.lastIndexOf('/');
			if (p > 0)
				pwd = path.substring(0, p);
			else
				pwd = "";
			fn = path.substring(p + 1);
		}
		lv[0].setSelection(updateList(fn));
		updateRecentList(recentFileList);
		vp.setCurrentItem(0);
	}

	private void updateRecentList(List<Config.ReadingInfo> recentFileList)
	{
		rfns.clear();
		for (Config.ReadingInfo ri : recentFileList) {
			HashMap<String, Object> m = new HashMap<String, Object>();
			m.put(LIST_HEAD_NAMES[0], R.drawable.icon_file);
			m.put(LIST_HEAD_NAMES[1], ri.name);
			m.put(LIST_HEAD_NAMES[2], ri.percent + "%");
			rfns.add(m);
		}
		sarf.notifyDataSetChanged();
		lv[1].setSelection(0);
	}

	private int updateList(@Nullable String filename)
	{
		String path = Reader.pathPrefix + "/" + pwd;

		fns.clear();
		HashMap<String, Object> map = new HashMap<String, Object>();

		VFile f = new VFile(path);
		List<VFile.Property> ps = f.listProperty();
		if (ps == null) {
			f = new VFile(Reader.pathPrefix);
			ps = f.listProperty();
			pwd = "";
		}

		// if not root
		if (pwd.length() > 0) {
			map.put("icon", R.drawable.icon_folder);
			map.put("name", upDir);
			fns.add(map);
		}

		int pos = -1;

		for (int i = 0; i < ps.size(); i++) {
			VFile.Property p = ps.get(i);
			map = new HashMap<String, Object>();
			if (p.isFile) {
				map.put(LIST_HEAD_NAMES[0], R.drawable.icon_file);
				map.put(LIST_HEAD_NAMES[2], "" + p.size / 1024 + " K");
			} else {
				map.put(LIST_HEAD_NAMES[0], R.drawable.icon_folder);
				map.put(LIST_HEAD_NAMES[2], "");
			}
			map.put(LIST_HEAD_NAMES[1], p.name);
			if ((filename != null) && (pos == -1))
				if (filename.equals(p.name))
					pos = i;
			fns.add(map);
		}

		saf.notifyDataSetChanged();
		setTitle(getContext().getString(R.string.dialog_file_title) + ((pwd.length() == 0) ? "/" : pwd));
		return pos;
	}

	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
	{
		int id = (Integer) fns.get(i).get("icon");
		String name = (String) fns.get(i).get("name");

		if (id == R.drawable.icon_file) {
			filePicked(pwd + "/" + name);
			return;
		}

		if (name.equals(upDir)) {
			int pos = pwd.lastIndexOf('/');
			//no slash, do nothing
			if (pos == -1)
				return;

			pwd = pwd.substring(0, pos);
		} else
			pwd = pwd + "/" + name;
		updateList(null);
	}

	private void filePicked(String result)
	{
		dismiss();
		fpl.onFilePicked(result);
	}
}
