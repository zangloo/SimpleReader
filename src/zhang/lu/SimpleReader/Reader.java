package zhang.lu.SimpleReader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Typeface;
import android.os.*;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.*;
import zhang.lu.SimpleReader.Book.BookContent;
import zhang.lu.SimpleReader.Book.BookLoader;
import zhang.lu.SimpleReader.Book.VFile;
import zhang.lu.SimpleReader.View.SimpleTextView;

import java.io.File;
import java.util.Date;
import java.util.List;

public class Reader extends Activity implements View.OnTouchListener, SimpleTextView.OnPosChangeListener
{
	public static final String ABOUT_MESSAGE = "<center>作者：<a href=\"http://weibo.com/2386922042\">zhanglu</a></center></br><center>主頁：<a href=\"http://sourceforge.net/projects/simplereader\">SimpleReader</a></center>";
	public static final String[] ReaderTip = {"", "", "請選取所需觀看的書本。書本須放置於SD卡中，books目錄下。", "所用字典請置于books下dict目錄中。", "如須使用其他字體替代自帶，可將字體置於books下fonts目錄中，字體可至“http://sourceforge.net/projects/vietunicode/files/hannom/hannom v2005/”下載"};

	public static final String pathPrefix = Environment.getExternalStorageDirectory() + "/books";
	public static final String dictPath = pathPrefix + "/dict/";
	public static final String dictSuffix = ".sqlite";
	public static final String fontPath = pathPrefix + "/fonts/";
	public static final String fontSuffix = ".ttf";

	public static final String DATE_FORMAT_STRING = "kk:mm";

	private static final int menuSearch = 0;
	private static final int menuBookmarkMgr = 1;
	private static final int menuExit = 2;
	private static final int menuDict = 3;
	private static final int menuBookmark = 4;
	private static final int menuChapterMgr = 5;
	private static final int menuSeek = 6;
	private static final int menuViewLock = 7;
	private static final int menuStatusBar = 8;
	private static final int menuColorBright = 9;
	private static final int menuFile = 10;
	private static final int menuOption = 11;
	private static final int menuAbout = 12;

	private static final int FILE_DIALOG_ID = 1;
	private static final int OPTION_DIALOG_ID = 2;

	// don't know how to get it , so define it for 4(3 for popupWindow board and 1 for scrollView board)
	private static final int POPUP_WINDOW_BOARD_SIZE = 4 * 2;

	private SimpleTextView hbv, xbv;
	private SimpleTextView bv;
	private Config config;
	private GestureDetector gs;
	private int currOrient = Configuration.ORIENTATION_UNDEFINED;
	private View sp = null;
	private View stp = null;
	private View skp = null;
	private EditText et = null;
	private SeekBar sb = null;
	private PopupWindow npw = null;
	private TextView nt = null;
	private FrameLayout nsv = null;
	private int ppi, ppo;
	private boolean loading = false;
	private SimpleTextView.FingerPosInfo fingerPosInfo = null;
	private Config.ReadingInfo ri = null;
	private BookmarkManager bookmarkManager = null;
	private ChapterManager chapterManager = null;
	private DictManager dictManager;
	private Typeface tf = null;
	private int screenWidth, screenHeight;

	private BroadcastReceiver timeTickReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent intent)
		{
			updateStatusPanelTime();
		}
	};

	private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent intent)
		{
			//int batteryIcon = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
			int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			//biv.setImageResource(batteryIcon);
			TextView tv = (TextView) findViewById(R.id.battery_level_text);
			tv.setText("  [" + batteryLevel + "%]  ");
		}
	};

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				     WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reader);

		// load config
		config = new Config(this);
		try {
			config.readConfig();
		} catch (SQLiteException e) {
			Util.errorMsg(this, R.string.error_open_config_file);
		}

		updateWH();
		// init panels
		initStatusPanel();
		initSearchPanel();
		initSeekBarPanel();
		initBookmarkMgr();
		initChapterMgr();
		initNote();

		// if external font exist, load it
		setTypeface(config.getFontFile());
		// init book view
		hbv = (SimpleTextView) findViewById(R.id.hbook_text);
		hbv.setOnTouchListener(this);
		xbv = (SimpleTextView) findViewById(R.id.xbook_text);
		xbv.setOnTouchListener(this);
		setView(config.isHanStyle());

		dictManager = new DictManager(this);
		setDictEnable(config.isDictEnabled());
		VFile.setDefaultEncode(config.getZipEncode());
		if (config.isShowStatus())
			showStatusPanel();
		setColorAndFont();
		setViewLock(config.getViewOrient());

		currOrient = getResources().getConfiguration().orientation;
		gs = new GestureDetector(new GestureDetector.OnGestureListener()
		{

			public boolean onDown(MotionEvent motionEvent)
			{
				return false;
			}

			public void onShowPress(MotionEvent motionEvent)
			{
			}

			public boolean onSingleTapUp(MotionEvent e)
			{
				String note = bv.getFingerPosNote(e.getX(), e.getY());
				if (note != null) {
					showNote(note, e);
					return true;
				}

				float p1, p2;
				switch (config.getPagingDirect()) {
					case clickUp:
						p1 = bv.getHeight() / 2;
						p2 = e.getY();
						break;
					case clickDown:
						p2 = bv.getHeight() / 2;
						p1 = e.getY();
						break;
					case clickRight:
						p2 = bv.getWidth() / 2;
						p1 = e.getX();
						break;
					case clickLeft:
						p1 = bv.getWidth() / 2;
						p2 = e.getX();
						break;
					default:
						return false;
				}
				if (p1 > p2)
					pageDown();
				else
					pageUp();
				return true;
			}

			public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1)
			{
				return false;
			}

			public void onLongPress(MotionEvent motionEvent)
			{
				if (config.getCurrFile() != null)
					fingerPosInfo = bv.getFingerPosInfo(motionEvent.getX(), motionEvent.getY());
				openOptionsMenu();
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float v, float v1)
			{
				float p1, p2;
				switch (config.getPagingDirect()) {
					case up:
						p1 = e1.getY();
						p2 = e2.getY();
						break;
					case down:
						p2 = e1.getY();
						p1 = e2.getY();
						break;
					case right:
						p2 = e1.getX();
						p1 = e2.getX();
						break;
					case left:
						p1 = e1.getX();
						p2 = e2.getX();
						break;
					default:
						return false;
				}
				if (p1 > p2)
					pageDown();
				else
					pageUp();
				return true;
			}
		});

		File f = new File(pathPrefix);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				Util.errorMsg(this, R.string.error_dir_not_exist);
				return;
			}
		} else if (!f.isDirectory()) {
			Util.errorMsg(this, R.string.error_dir_is_file);
			return;
		}

		//every thing is ok, load last file if have
		openfile(config.getCurrFile());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		currOrient = newConfig.orientation;
		updateWH();
		if (bookmarkManager.isShowing())
			bookmarkManager.update(screenWidth >> 1, screenHeight);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed()
	{
		if (hidePanels()) {
			bv.setPos(ppi, ppo);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public boolean onSearchRequested()
	{
		showSearchPanel();
		return false;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(timeTickReceiver);
		unregisterReceiver(batteryChangedReceiver);
		saveReadingInfo();
		try {
			config.saveConfig();
		} catch (SQLiteException e) {
			Util.errorMsg(this, R.string.error_write_config);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		updateStatusPanelTime();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		bv.setContent(null);
		bv.setPos(0, 0);
		config.close();
		dictManager.unloadDict();
		BookLoader.unloadBook();
	}

	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
			case FILE_DIALOG_ID:
				FileDialog fd = new FileDialog(this);
				fd.init(new FileDialog.OnFilePickedListener()
				{
					public void onFilePicked(String filename)
					{
						if (!filename.equals(config.getCurrFile()))
							openfile(filename);
					}
				});
				return fd;
			case OPTION_DIALOG_ID:
				OptionDialog od = new OptionDialog(this);
				od.init(new OptionDialog.OnOptionAcceptListener()
				{
					public void onOptionAccept(Config cfg)
					{
						boolean han = config.isHanStyle();
						config.getback(cfg);
						VFile.setDefaultEncode(config.getZipEncode());
						setTypeface(config.getFontFile());

						if (config.isHanStyle() != han) {
							setView(config.isHanStyle());
							initNote();
						}
						setColorAndFont();
						setDictEnable(config.isDictEnabled());
						bv.invalidate();
					}
				});
				return od;
			default:
				return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id) {
			case FILE_DIALOG_ID:
				((FileDialog) dialog).update(config.getCurrFile(), config.getRecentFilesList());
				break;
			case OPTION_DIALOG_ID:
				((OptionDialog) dialog).update(config.dup());
				break;
			default:
		}
	}

	private void showNote(String note, MotionEvent e)
	{
		nt.setText(note);

		if (config.isHanStyle()) {
			nt.measure(View.MeasureSpec.UNSPECIFIED, (screenHeight >> 1) + View.MeasureSpec.AT_MOST);
			int w = Math.min(nt.getMeasuredWidth(), screenWidth >> 1) + POPUP_WINDOW_BOARD_SIZE;
			npw.setWidth(w);
			npw.setHeight(Math.min(nt.getMeasuredHeight(), screenHeight >> 1) + POPUP_WINDOW_BOARD_SIZE);
			// this code block is dirty, so ...
			// if anyone know any better way that can make scrollTo take effect, tell me
			nsv.postDelayed(new Runnable()
			{
				public void run()
				{
					nsv.scrollTo(nt.getMeasuredWidth(), 0);
				}
			}, 10);
			// not work, so do it manually. if you know why, tell me please
			// npw.showAtLocation(bv, Gravity.TOP | Gravity.RIGHT, (int) e.getRawX(), (int) e.getRawY());
			npw.showAtLocation(bv, Gravity.NO_GRAVITY, (int) e.getRawX() - w + 2/* scrollView board*/,
					   (int) e.getRawY());
		} else {
			nt.measure((screenWidth >> 1) + View.MeasureSpec.AT_MOST, View.MeasureSpec.UNSPECIFIED);
			int w = Math.min(nt.getMeasuredWidth(), screenWidth >> 1) + POPUP_WINDOW_BOARD_SIZE;
			npw.setWidth(w);
			npw.setHeight(Math.min(nt.getMeasuredHeight(), screenHeight >> 1) + POPUP_WINDOW_BOARD_SIZE);
			nsv.scrollTo(0, 0);
			npw.showAtLocation(bv, Gravity.TOP | Gravity.LEFT, (int) e.getRawX(), (int) e.getRawY());
		}
	}

	private void setDictEnable(boolean de)
	{
		if (de)
			dictManager.loadDict(dictPath + config.getDictFile() + dictSuffix, config.getDictFile());
		else
			dictManager.unloadDict();
	}

	private void hideSearchPanel()
	{
		bv.setHighlightInfo(null);
		sp.setEnabled(false);
		sp.setVisibility(View.GONE);
	}

	private void showSearchPanel()
	{
		ppi = bv.getPosIndex();
		ppo = bv.getPosOffset();
		sp.setVisibility(View.VISIBLE);
		sp.setEnabled(true);
		et.requestFocus();
	}

	private void hideSeekPanel()
	{
		skp.setEnabled(false);
		skp.setVisibility(View.GONE);
	}

	private void showSeekPanel()
	{
		ppi = bv.getPosIndex();
		ppo = bv.getPosOffset();
		skp.setVisibility(View.VISIBLE);
		skp.setEnabled(true);
		sb.setProgress(bv.getPos());
	}

	private void hideStatusPanel()
	{
		stp.setEnabled(false);
		stp.setVisibility(View.GONE);
		unregisterReceiver(timeTickReceiver);
		unregisterReceiver(batteryChangedReceiver);
		bv.setOnPosChangeListener(null);
	}

	private void showStatusPanel()
	{
		updateStatusPanel();
		updateStatusPanelTime();

		registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		bv.setOnPosChangeListener(this);
	}

	private void updateStatusPanel()
	{
		int c, bc;
		c = config.getCurrentColor();
		bc = config.getCurrentBColor();

		TextView tv = (TextView) findViewById(R.id.reading_percent_text);
		tv.setTextColor(c);
		tv.setBackgroundColor(bc);

		tv = (TextView) findViewById(R.id.reading_book_text);
		tv.setTextColor(c);
		tv.setBackgroundColor(bc);

		tv = (TextView) findViewById(R.id.reading_time_text);
		tv.setTextColor(c);
		tv.setBackgroundColor(bc);

		tv = (TextView) findViewById(R.id.battery_level_text);
		tv.setTextColor(c);
		tv.setBackgroundColor(bc);

		stp.setBackgroundColor(bc);
		stp.setEnabled(true);
		stp.setVisibility(View.VISIBLE);
	}

	private void initStatusPanel()
	{
		stp = findViewById(R.id.status_panel);
		TextView tv = (TextView) findViewById(R.id.reading_book_text);
		tv.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				showDialog(FILE_DIALOG_ID);
			}
		});

		tv = (TextView) findViewById(R.id.reading_percent_text);
		tv.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				showSeekPanel();
			}
		});

		tv = (TextView) findViewById(R.id.reading_time_text);
		View.OnClickListener ocl = new View.OnClickListener()
		{
			public void onClick(View view)
			{
				switchColorBright();
			}
		};
		tv.setOnClickListener(ocl);

		tv = (TextView) findViewById(R.id.battery_level_text);
		tv.setOnClickListener(ocl);
	}

	private void updateStatusPanelTime()
	{
		if (!config.isShowStatus())
			return;
		TextView tv = (TextView) findViewById(R.id.reading_time_text);
		tv.setText(DateFormat.format(DATE_FORMAT_STRING, new Date(System.currentTimeMillis())));
	}

	private void updateStatusPanelFile(BookContent book)
	{
		if (!config.isShowStatus())
			return;
		TextView tv = (TextView) findViewById(R.id.reading_book_text);
		String n = config.getCurrFile();
		n = n.substring(n.lastIndexOf('/') + 1);
		if (book.getChapterCount() > 1)
			n += "#" + book.getChapterTitle();
		tv.setText(n);
	}

	private void setColorAndFont()
	{
		bv.setColorAndFont(config.getCurrentColor(), config.getCurrentBColor(), config.getFontSize(), tf);
		updateStatusPanel();
		nt.setTypeface(tf);
		nt.invalidate();
	}

	private void setViewLock(int orient)
	{
		Util.setActivityOrient(this, orient);
	}

	private void setView(boolean han)
	{
		if (han) {
			bv = hbv;
			hbv.setVisibility(View.VISIBLE);
			xbv.setVisibility(View.GONE);
		} else {
			bv = xbv;
			hbv.setVisibility(View.GONE);
			xbv.setVisibility(View.VISIBLE);
		}
		if (config.isShowStatus())
			bv.setOnPosChangeListener(this);
		else
			bv.setOnPosChangeListener(null);
	}

	private void switchColorBright()
	{
		config.setColorBright(!config.isColorBright());
		setColorAndFont();
		if (config.isShowStatus())
			showStatusPanel();
		bv.invalidate();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case menuFile:
				showDialog(FILE_DIALOG_ID);
				break;
			case menuSearch:
				showSearchPanel();
				break;
			case menuOption:
				showDialog(OPTION_DIALOG_ID);
				break;
			case menuViewLock:
				if (config.isViewLock())
					config.unsetViewOrient();
				else
					config.setViewOrient(currOrient);
				setViewLock(config.getViewOrient());
				break;
			case menuBookmarkMgr:
				if (config.getCurrFile() != null)
					bookmarkManager.show(ri, bv.getContent(), tf, bv.getTop(), (screenWidth >> 1),
							     screenHeight - bv.getTop());
				break;
			case menuChapterMgr:
				if (config.getCurrFile() != null)
					chapterManager.show(bv.getContent().getChapterTitleList(),
							    bv.getContent().getCurrChapter(), tf, bv.getTop(),
							    (screenWidth >> 1), screenHeight - bv.getTop());
				break;
			case menuSeek:
				showSeekPanel();
				break;
			case menuColorBright:
				switchColorBright();
				break;
			case menuStatusBar:
				if (config.isShowStatus()) {
					config.setShowStatus(false);
					hideStatusPanel();
				} else {
					config.setShowStatus(true);
					showStatusPanel();
					updateStatusPanelFile(bv.getContent());
				}
				break;
			case menuDict:
				assert fingerPosInfo != null;
				assert fingerPosInfo.str != null;
				assert fingerPosInfo.str.length() > 0;

				if (dictManager.getDictMaxWordLen() < fingerPosInfo.str.length())
					fingerPosInfo.str = fingerPosInfo.str
						.substring(0, dictManager.getDictMaxWordLen());
				dictManager.showDict(fingerPosInfo);
				break;
			case menuBookmark:
				assert fingerPosInfo != null;
				assert fingerPosInfo.str != null;
				assert fingerPosInfo.str.length() > 0;

				if (ri != null) {
					if (BookmarkManager.BOOKMARK_DESC_DEFAULT_LEN < fingerPosInfo.str.length())
						fingerPosInfo.str = fingerPosInfo.str
							.substring(0, BookmarkManager.BOOKMARK_DESC_DEFAULT_LEN);

					bookmarkManager.addDialog(BookmarkManager.createBookmark(fingerPosInfo, ri));
				}
				break;
			case menuExit:
				finish();
				break;
			case menuAbout:
				Util.showDialog(this, ABOUT_MESSAGE, R.string.about_title);
				break;
		}
		return true;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu)
	{
		fingerPosInfo = null;
		super.onOptionsMenuClosed(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, menuFile, menuFile, getResources().getString(R.string.menu_file));
		if (config.isViewLock())
			menu.add(0, menuViewLock, menuViewLock, getResources().getString(R.string.menu_unlock_view));
		else
			menu.add(0, menuViewLock, menuViewLock, getResources().getString(R.string.menu_lock_view));
		menu.add(0, menuDict, menuDict, getResources().getString(R.string.menu_dict));
		menu.add(0, menuBookmark, menuBookmark, getResources().getString(R.string.menu_bookmark));
		menu.add(0, menuColorBright, menuColorBright, getResources()
			.getString(!config.isColorBright() ? R.string.color_mode_day : R.string.color_mode_night));
		menu.add(0, menuStatusBar, menuStatusBar, getResources().getString(R.string.menu_status_bar));
		menu.add(0, menuExit, menuExit, getResources().getString(R.string.menu_exit));
		menu.add(0, menuOption, menuOption, getResources().getString(R.string.menu_option));
		menu.add(0, menuAbout, menuAbout, getResources().getString(R.string.menu_about));
		menu.add(0, menuSearch, menuSearch, getResources().getString(R.string.menu_search));
		menu.add(0, menuBookmarkMgr, menuBookmarkMgr, getResources().getString(R.string.menu_bookmark_mgr));
		menu.add(0, menuSeek, menuSeek, getResources().getString(R.string.menu_seek));
		menu.add(0, menuChapterMgr, menuChapterMgr, getResources().getString(R.string.menu_chapter));

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (config.isViewLock())
			menu.findItem(menuViewLock).setTitle(R.string.menu_unlock_view);
		else
			menu.findItem(menuViewLock).setTitle(R.string.menu_lock_view);
		boolean de = config.isDictEnabled();
		char c = 0;
		MenuItem mi;
		if ((fingerPosInfo != null) && (fingerPosInfo.str != null) && (fingerPosInfo.str.length() > 0)) {
			c = fingerPosInfo.str.charAt(0);
			mi = menu.findItem(menuBookmark).setVisible(true);
			mi.setTitle(getString(R.string.menu_bookmark) + c);
		} else {
			menu.findItem(menuBookmark).setVisible(false);
			de = false;
		}
		menu.findItem(menuChapterMgr).setVisible(bv.getContent().getChapterCount() > 1);

		mi = menu.findItem(menuDict).setVisible(de);
		if (de)
			mi.setTitle(getString(R.string.menu_dict) + c);

		menu.findItem(menuColorBright)
		    .setTitle(!config.isColorBright() ? R.string.color_mode_day : R.string.color_mode_night);
		return true;
	}

	public boolean onTouch(View view, MotionEvent e)
	{
		gs.onTouchEvent(e);
		return true;
	}

	public void onPosChange(int pos, boolean fromUser)
	{
		TextView tv = (TextView) findViewById(R.id.reading_percent_text);
		tv.setText("  " + pos + "%");

		if (isSeekPanelOn())
			sb.setProgress(pos);
	}

	private void pageDown()
	{
		if (bv.pageDown())
			return;

		if (loading)
			return;

		loading = true;
		String path = config.getCurrFile();
		if (path == null) {
			loading = false;
			return;
		}

		BookContent book = bv.getContent();
		if (book.getChapterCount() > 1)
			if (book.gotoChapter(book.getCurrChapter() + 1)) {
				switchChapterUpdate(book, 0, 0);
				loading = false;
				return;
			}
		String pwd, fn;
		int pos = path.lastIndexOf('/');
		if (pos > 0)
			pwd = path.substring(0, pos);
		else
			pwd = "";
		fn = path.substring(pos + 1);

		VFile f = new VFile(pathPrefix + pwd);
		List<VFile.Property> ps = f.listProperty(false);
		if (ps == null) {
			loading = false;
			return;
		}

		int i;
		for (i = 0; i < ps.size(); i++)
			if (fn.equals(ps.get(i).name))
				break;
		i++;
		if (i >= ps.size()) {
			loading = false;
			return;
		}

		//change book, so save reading info
		saveReadingInfo();
		openfile(pwd + '/' + ps.get(i).name);
	}

	private void pageUp()
	{
		if (bv.pageUp())
			return;

		if (loading)
			return;

		loading = true;
		String path = config.getCurrFile();
		if (path == null) {
			loading = false;
			return;
		}

		BookContent book = bv.getContent();
		if (book.getChapterCount() > 1)
			if (book.gotoChapter(book.getCurrChapter() - 1)) {
				bv.gotoEnd();
				switchChapterUpdate(book, bv.getPosIndex(), bv.getPosOffset());
				loading = false;
				return;
			}
		String pwd, fn;
		int pos = path.lastIndexOf('/');
		if (pos > 0)
			pwd = path.substring(0, pos);
		else
			pwd = "";
		fn = path.substring(pos + 1);

		VFile f = new VFile(pathPrefix + pwd);
		List<VFile.Property> ps = f.listProperty(false);
		if (ps == null) {
			loading = false;
			return;
		}

		int i;
		for (i = 0; i < ps.size(); i++)
			if (fn.equals(ps.get(i).name))
				break;
		if (i <= 0) {
			loading = false;
			return;
		}
		i--;

		//change book, so save reading info
		saveReadingInfo();
		openfile(pwd + '/' + ps.get(i).name);
	}

	private void saveReadingInfo()
	{
		if (ri == null)
			return;
		ri.chapter = bv.getContent().getCurrChapter();
		ri.line = bv.getPosIndex();
		ri.offset = bv.getPosOffset();
		ri.percent = bv.getPos();
		config.setReadingInfo(ri);
	}

	ProgressDialog pd;
	private final Handler handler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			pd.dismiss();

			if (msg.arg1 == 0) {
				String fp = (String) msg.getData().get("filename");
				config.removeReadingInfo(fp);

				Util.errorMsg(Reader.this, getString(R.string.error_open_file) + fp);
			} else {

				ppi = ri.line;
				ppo = ri.offset;
				if (bv.getContent().getChapterCount() > 1)
					bv.getContent().gotoChapter(ri.chapter);
				bv.setPos(ppi, ppo);

				hidePanels();
				updateStatusPanelFile(bv.getContent());
				bv.invalidate();
			}
			loading = false;
		}
	};

	private void openfile(final String fp)
	{
		if (fp == null)
			return;
		loading = true;
		pd = ProgressDialog.show(this, getString(R.string.loading), fp, true);
		Thread thread = new Thread(new Runnable()
		{
			public void run()
			{
				Message msg;
				msg = handler.obtainMessage();

				saveReadingInfo();
				BookContent bc = BookLoader.loadFile(pathPrefix + fp);
				if (bc != null) {
					config.setReadingFile(fp);
					bv.setContent(bc);
					ri = config.getReadingInfo(config.getCurrFile());
					msg.arg1 = 1;
				} else {
					Bundle b = new Bundle();
					b.putString("filename", fp);
					msg.setData(b);
					msg.arg1 = 0;
				}

				handler.sendMessage(msg);
			}
		});
		thread.start();
	}

	private boolean isSeekPanelOn()
	{
		return skp.getVisibility() == View.VISIBLE;
	}

	private boolean isSearchPanelOn()
	{
		return sp.getVisibility() == View.VISIBLE;
	}

	private void initSearchPanel()
	{
		sp = findViewById(R.id.search_panel);
		et = (EditText) findViewById(R.id.search_text_edit);
		Button btn = (Button) findViewById(R.id.search_button_search);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				String s = et.getText().toString();
				BookContent.ContentPosInfo sr = bv.searchText(s);
				if (sr != null) {
					bv.setPos(sr.line, sr.offset);
					bv.setHighlightInfo(new SimpleTextView.HighlightInfo(sr.line, sr.offset,
											     sr.offset + s.length()));
					bv.invalidate();
				}
			}
		});
		btn = (Button) findViewById(R.id.search_button_ok);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				hideSearchPanel();
			}
		});
		btn = (Button) findViewById(R.id.search_button_cancel);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				bv.setPos(ppi, ppo);
				hideSearchPanel();
			}
		});
	}

	private void initSeekBarPanel()
	{
		sb = (SeekBar) findViewById(R.id.seek_seekbar);
		skp = findViewById(R.id.seek_panel);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{

			public void onProgressChanged(SeekBar seekBar, int i, boolean b)
			{
				if ((!b) || (config.getCurrFile() == null))
					return;

				bv.setPos(i);
				bv.invalidate();
			}

			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
		});

		Button btn = (Button) findViewById(R.id.seek_button_ok);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				hideSeekPanel();
			}
		});

		btn = (Button) findViewById(R.id.seek_button_cancel);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				bv.setPos(ppi, ppo);
				hideSeekPanel();
			}
		});
	}

	private void initChapterMgr()
	{
		chapterManager = new ChapterManager(this, new ChapterManager.OnChapterSelectListener()
		{
			public void onChapterSelect(int chapter)
			{
				chapterManager.hide();
				BookContent book = bv.getContent();
				book.gotoChapter(chapter);
				switchChapterUpdate(book, 0, 0);
			}
		});
	}

	private void initBookmarkMgr()
	{
		bookmarkManager = new BookmarkManager(this, config, new BookmarkManager.OnBookmarkSelectListener()
		{
			public void onBookmarkSelect(BookmarkManager.Bookmark bookmark)
			{
				bookmarkManager.hide();
				if (config.getCurrFile() == null)
					return;
				BookContent book = bv.getContent();
				ppi = bookmark.line;
				ppo = bookmark.offset;
				if (book.getCurrChapter() != bookmark.chapter) {
					book.gotoChapter(bookmark.chapter);
					switchChapterUpdate(book, ppi, ppo);
				} else {
					bv.setPos(ppi, ppo);
					bv.invalidate();
				}
			}
		});
	}

	private boolean hidePanels()
	{
		boolean ret = false;
		if (isSeekPanelOn()) {
			hideSeekPanel();
			ret = true;
		}
		if (isSearchPanelOn()) {
			hideSearchPanel();
			ret = true;
		}
		return ret;
	}

	private void setTypeface(String name)
	{
		if (name != null) {
			String fn = fontPath + name + fontSuffix;
			File ff = new File(fn);
			if (ff.exists())
				tf = Typeface.createFromFile(fn);
		} else
			tf = null;
	}

	private void updateWH()
	{
		screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		screenHeight = getWindowManager().getDefaultDisplay().getHeight();
	}

	private void initNote()
	{
		int id;
		if (config.isHanStyle())
			id = R.layout.hnotedlg;
		else
			id = R.layout.notedlg;
		View v = getLayoutInflater().inflate(id, null, true);

		npw = new PopupWindow(this);
		npw.setContentView(v);
		npw.setWidth((screenWidth >> 1) + POPUP_WINDOW_BOARD_SIZE);
		npw.setHeight((screenWidth >> 1) + POPUP_WINDOW_BOARD_SIZE);
		npw.setFocusable(true);
		nt = (TextView) v.findViewById(R.id.note_text);
		nsv = (FrameLayout) v.findViewById(R.id.note_scroll);
	}

	private void switchChapterUpdate(BookContent book, int line, int offset)
	{
		ppi = line;
		ppo = offset;
		bv.setPos(ppi, ppo);
		ri.chapter = book.getCurrChapter();

		updateStatusPanel();
		updateStatusPanelFile(book);
		bv.invalidate();
	}
}
