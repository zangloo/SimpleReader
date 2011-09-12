package zhang.lu.SimpleReader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
public class FileDialog extends Activity implements AdapterView.OnItemClickListener
{
	private static final String upDir = "..";
	public static final String RESULT_FILE_NAME = "filename";
	public static final char posSplitter = ',';

	private static final String[] LIST_HEAD_NAMES = new String[]{"icon", "name", "info"};
	private static final int[] LIST_HEAD_ID = new int[]{R.id.file_icon, R.id.file_name, R.id.file_info};

	private List<HashMap<String, Object>> fns;
	private List<String> rfl = new ArrayList<String>();
	private SimpleAdapter sa;
	private ListView[] lv = new ListView[2];
	private ViewPager vp;
	private RadioGroup rg;
	private String pwd;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				     WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.filedlg);

		Bundle bundle = getIntent().getExtras();
		String path = bundle.getString(Reader.BUNDLE_DATA_NAME_PATH);
		Util.setActivityOrient(this, bundle.getInt(Reader.BUNDLE_DATA_CURR_ORIENT));

		// setup recently files list
		lv[1] = new ListView(this);
		fns = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < Config.MAX_RECENTLY_FILE_COUNT; i++) {
			String s = (String) bundle.get(Config.RECENTLY_FILE_PREFIX + (i + 1));
			if (s == null)
				continue;
			int p = s.indexOf(posSplitter);
			HashMap<String, Object> m = new HashMap<String, Object>();
			m.put(LIST_HEAD_NAMES[0], null);
			m.put(LIST_HEAD_NAMES[2], s.substring(0, p) + "%");
			s = s.substring(p + 1);
			m.put(LIST_HEAD_NAMES[1], s);
			rfl.add(s);
			fns.add(m);
		}
		sa = new SimpleAdapter(this, fns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[1].setAdapter(sa);
		lv[1].setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				filePicked(rfl.get(position));
			}
		});

		// setup file list
		fns = new ArrayList<HashMap<String, Object>>();
		sa = new SimpleAdapter(this, fns, R.layout.filelist, LIST_HEAD_NAMES, LIST_HEAD_ID);
		lv[0] = new ListView(this);
		lv[0].setAdapter(sa);
		lv[0].setOnItemClickListener(this);

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
				setResult(RESULT_CANCELED, null);
				finish();
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

		sa.notifyDataSetChanged();
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
		Intent data = new Intent();
		data.putExtra(RESULT_FILE_NAME, result);
		setResult(RESULT_OK, data);
		finish();
	}
}
