package net.lzrj.SimpleReader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.jetbrains.annotations.Nullable;
import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.R;
import net.lzrj.SimpleReader.vfs.VFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Zhanglu
 * Date: 10-12-4
 * Time: 下午8:58
 */
public class FileDialog extends Dialog
{
	public interface OnFilePickedListener
	{
		void onFilePicked(String filename);
	}

	private static final String upDir = "..";

	private static final String[] LIST_HEAD_NAMES = new String[]{"icon", "name", "title", "info"};
	private static final int[] LIST_HEAD_ID = new int[]{R.id.file_icon, R.id.file_name, R.id.title_name, R.id.file_info};

	private List<HashMap<String, Object>> fns;
	private List<HashMap<String, Object>> rfns;
	private List<HashMap<String, Object>> ofns;
	private SimpleAdapter saf;
	private SimpleAdapter saof;
	private SimpleAdapter sarf;
	private final ListView[] lv = new ListView[3];
	private ViewPager vp;
	private PagerAdapter pa;
	private RadioGroup rg;
	private String pwd;
	private String opwd;
	private boolean showOnline = false;
	// ViewPager can't set currPage when not inited, do it late
	private int currPage = -1;

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
		rfns = new ArrayList<>();
		sarf = new SimpleAdapter(getContext(), rfns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID)
		{
			@Override
			public View getView(int position, View view, ViewGroup parent)
			{
				View v = super.getView(position, view, parent);
				TextView tv = (TextView) v.findViewById(R.id.title_name);
				if (tv.getText().length() > 0)
					tv.setVisibility(View.VISIBLE);
				else
					tv.setVisibility(View.GONE);
				return v;
			}
		};
		lv[1].setAdapter(sarf);
		lv[1].setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				filePicked((String) rfns.get(position).get("name"));
			}
		});

		// setup file list
		fns = new ArrayList<>();
		saf = new SimpleAdapter(getContext(), fns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[0] = new ListView(getContext());
		lv[0].setAdapter(saf);
		lv[0].setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Map map = (Map) adapterView.getItemAtPosition(i);
				int id = (Integer) map.get("icon");
				String name = (String) map.get("name");

				if ((pwd = processPath(pwd, name, id)) != null)
					updateList(null);
			}

		});

		// setup online file list
		ofns = new ArrayList<>();
		saof = new SimpleAdapter(getContext(), ofns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[2] = new ListView(getContext());
		lv[2].setAdapter(saof);
		lv[2].setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Map map = (Map) adapterView.getItemAtPosition(i);
				int id = (Integer) map.get("icon");
				String name = (String) map.get("name");

				if ((opwd = processPath(opwd, name, id)) != null)
					updateOnlineList(null);
			}

		});

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
					case 2:
						rg.check(R.id.button_file_online_list);
						break;
				}
				updateTitle(i);
			}

			public void onPageScrollStateChanged(int i)
			{
			}
		});
		pa = new PagerAdapter()
		{
			@Override
			public int getCount()
			{
				return showOnline ? 3 : 2;
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
				if (currPage == -1)
					return;
				vp.setCurrentItem(currPage);
				currPage = -1;
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
		};
		vp.setAdapter(pa);

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
					case R.id.button_file_online_list:
						vp.setCurrentItem(2);
						break;
				}
			}
		});
	}

	public void update(String path, List<Config.ReadingInfo> recentFileList, boolean showOnline)
	{
		String fn = null;
		//if path is root, it should be "" not "/"
		pwd = "";
		opwd = VFile.CLOUD_FILE_PREFIX;
		if (VFile.isCloudFile(path)) {
			if (path != null) {
				int p = path.lastIndexOf('/');
				if (p > 0)
					opwd = path.substring(0, p);
				fn = path.substring(p + 1);
			}
		} else if (path != null) {
			int p = path.lastIndexOf('/');
			if (p > 0)
				pwd = path.substring(0, p);
			fn = path.substring(p + 1);
		}

		rg.findViewById(R.id.button_file_online_list).setVisibility(showOnline ? View.VISIBLE : View.GONE);
		this.showOnline = showOnline;
		pa.notifyDataSetChanged();
		if ((showOnline) && (VFile.isCloudFile(path))) {
			lv[0].setSelection(updateList(null));
			lv[2].setSelection(updateOnlineList(fn));
			currPage = 2;
			updateTitle(2);
		} else {
			lv[0].setSelection(updateList(fn));
			if (showOnline)
				lv[2].setSelection(updateOnlineList(null));
			currPage = 0;
			updateTitle(0);
		}
		updateRecentList(recentFileList);
	}

	private void updateRecentList(List<Config.ReadingInfo> recentFileList)
	{
		rfns.clear();
		for (Config.ReadingInfo ri : recentFileList) {
			HashMap<String, Object> m = new HashMap<>();
			m.put(LIST_HEAD_NAMES[0], R.drawable.icon_file);
			m.put(LIST_HEAD_NAMES[1], ri.name);
			m.put(LIST_HEAD_NAMES[2], ri.ctitle);
			m.put(LIST_HEAD_NAMES[3], ri.percent + "%");
			rfns.add(m);
		}
		sarf.notifyDataSetChanged();
		lv[1].setSelection(0);
	}

	private int updateList(@Nullable String filename)
	{
		fns.clear();
		HashMap<String, Object> map = new HashMap<>();

		VFile f = VFile.create(pwd);
		List<VFile.Property> ps = f.listProperty();
		if (ps == null) {
			f = VFile.create("");
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

		if (ps == null)
			return 0;
		for (int i = 0; i < ps.size(); i++) {
			VFile.Property p = ps.get(i);
			map = new HashMap<>();
			if (p.isFile) {
				map.put(LIST_HEAD_NAMES[0], R.drawable.icon_file);
				map.put(LIST_HEAD_NAMES[3], "" + p.size / 1024 + " K");
			} else {
				map.put(LIST_HEAD_NAMES[0], R.drawable.icon_folder);
				map.put(LIST_HEAD_NAMES[3], "");
			}
			map.put(LIST_HEAD_NAMES[1], p.name);
			map.put(LIST_HEAD_NAMES[2], "");
			if ((filename != null) && (pos == -1))
				if (filename.equals(p.name))
					pos = i;
			fns.add(map);
		}

		saf.notifyDataSetChanged();
		updateTitle(0);
		return pos;
	}

	private int updateOnlineList(@Nullable String filename)
	{
		ofns.clear();
		HashMap<String, Object> map = new HashMap<>();

		VFile f = VFile.create(opwd);
		List<VFile.Property> ps = f.listProperty();

		if (ps == null) {
			opwd = VFile.CLOUD_FILE_PREFIX;
			f = VFile.create(opwd);
			ps = f.listProperty();
		}

		int pos = -1;
		if (ps != null) {
			// if not root
			if (opwd.length() > VFile.CLOUD_FILE_PREFIX.length()) {
				map.put("icon", R.drawable.icon_folder);
				map.put("name", upDir);
				ofns.add(map);
			}

			for (int i = 0; i < ps.size(); i++) {
				VFile.Property p = ps.get(i);
				map = new HashMap<>();
				if (p.isFile) {
					map.put(LIST_HEAD_NAMES[0], R.drawable.icon_file);
					map.put(LIST_HEAD_NAMES[3], "" + p.size / 1024 + " K");
				} else {
					map.put(LIST_HEAD_NAMES[0], R.drawable.icon_folder);
					map.put(LIST_HEAD_NAMES[3], "");
				}
				map.put(LIST_HEAD_NAMES[1], p.name);
				map.put(LIST_HEAD_NAMES[2], "");
				if ((filename != null) && (pos == -1))
					if (filename.equals(p.name))
						pos = i;
				ofns.add(map);
			}
		}
		saof.notifyDataSetChanged();
		updateTitle(2);
		return pos;
	}

	private void updateTitle(int index)
	{
		switch (index) {
			case 0:
				setTitle(getContext().getString(R.string.dialog_file_title) +
					((pwd.length() == 0) ? "/" : pwd));
				break;
			case 1:
				setTitle(getContext().getString(R.string.dialog_file_title));
				break;
			case 2:
				setTitle(getContext().getString(R.string.dialog_file_title) +
					(opwd.equals(VFile.CLOUD_FILE_PREFIX) ?
						VFile.CLOUD_FILE_PREFIX + "/" : opwd));
				break;
		}
	}

	private String processPath(String p, String n, int id)
	{
		if (id == R.drawable.icon_file) {
			filePicked(p + "/" + n);
			return null;
		}

		if (n.equals(upDir)) {
			int pos = p.lastIndexOf('/');
			//no slash, do nothing
			if (pos == -1)
				return null;

			p = p.substring(0, pos);
		} else
			p = p + "/" + n;
		return p;
	}

	private void filePicked(String result)
	{
		dismiss();
		fpl.onFilePicked(result);
	}
}
