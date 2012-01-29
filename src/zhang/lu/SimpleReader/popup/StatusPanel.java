package zhang.lu.SimpleReader.Popup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.R;

import java.util.Date;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-10-7
 * Time: 上午10:47
 */
public class StatusPanel extends PopupWindow
{
	public static interface OnPanelClickListener
	{
		void onFilenameClick();

		void onPosClick();

		void onPosChanged(Config.ReadingInfo ri);

		void onBatteryLevelClick();
	}

	public static final String DATE_FORMAT_STRING = "kk:mm";
	public static final int POPUP_WIN_BOARD_SIZE = 3 * 2;

	private OnPanelClickListener pcl;
	private View layout;
	private TextView ptv, ftv, btv, ttv, ctv;
	private Button pbt, nbt;
	private Stack<Config.ReadingInfo> ris;
	private Stack<Config.ReadingInfo> nis = new Stack<Config.ReadingInfo>();
	private Config.ReadingInfo cri;

	private BroadcastReceiver timeTickReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent intent)
		{
			updateTime();
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
			btv.setText("[" + batteryLevel + "%]");
		}
	};

	public StatusPanel(Context context, OnPanelClickListener onPanelClickListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.statuspanel, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(100);
		setWidth(100);

		pcl = onPanelClickListener;

		ftv = (TextView) layout.findViewById(R.id.reading_book_text);
		ctv = (TextView) layout.findViewById(R.id.reading_chapter_text);
		ptv = (TextView) layout.findViewById(R.id.reading_percent_text);
		ttv = (TextView) layout.findViewById(R.id.reading_time_text);
		btv = (TextView) layout.findViewById(R.id.battery_level_text);

		pbt = (Button) layout.findViewById(R.id.button_prev_pos);
		nbt = (Button) layout.findViewById(R.id.button_next_pos);
		pbt.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				nis.push(cri);
				cri = ris.pop();
				updateReadingInfo(cri);
				pbt.setEnabled(ris.size() > 0);
				nbt.setEnabled(true);
				pcl.onPosChanged(cri);
			}
		});
		nbt.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				ris.push(cri);
				cri = nis.pop();
				updateReadingInfo(cri);
				pbt.setEnabled(true);
				nbt.setEnabled(nis.size() > 0);
				pcl.onPosChanged(cri);
			}
		});

		Button btn = (Button) layout.findViewById(R.id.button_file);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				pcl.onFilenameClick();
			}
		});

		btn = (Button) layout.findViewById(R.id.button_pos);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				pcl.onPosClick();
			}
		});

		btn = (Button) layout.findViewById(R.id.button_bright_mode);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				pcl.onBatteryLevelClick();
			}
		});
	}

	public void show(Stack<Config.ReadingInfo> ris, Config.ReadingInfo ri, int width)
	{
		this.ris = ris;
		nis.clear();
		cri = ri;
		pbt.setEnabled(ris.size() > 0);
		nbt.setEnabled(false);

		updateReadingInfo(ri);
		updateTime();

		Context context = layout.getContext();
		context.registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		context.registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		setWidth(width);
		layout.measure(width + View.MeasureSpec.EXACTLY, 0);
		setHeight(layout.getMeasuredHeight() + POPUP_WIN_BOARD_SIZE);
		showAtLocation(layout, Gravity.TOP | Gravity.CENTER, 0, 0);
	}

	public void hide()
	{
		dismiss();
	}

	@Override
	public void dismiss()
	{
		super.dismiss();
		Context context = layout.getContext();
		context.unregisterReceiver(timeTickReceiver);
		context.unregisterReceiver(batteryChangedReceiver);
	}

	@Override
	public void update(int width, int height)
	{
		layout.measure(width + View.MeasureSpec.EXACTLY, 0);
		super.update(width, layout.getMeasuredHeight() + POPUP_WIN_BOARD_SIZE);
	}

	private void updateTime()
	{
		ttv.setText(DateFormat.format(DATE_FORMAT_STRING, new Date(System.currentTimeMillis())));
	}

	private void updateReadingInfo(Config.ReadingInfo ri)
	{
		if (ri == null)
			return;
		ftv.setText(ri.name);
		if (ri.ctitle != null) {
			ctv.setText(ri.ctitle);
			ctv.setVisibility(View.VISIBLE);
		} else
			ctv.setVisibility(View.GONE);
		ptv.setText(ri.percent + "%");
	}
}
