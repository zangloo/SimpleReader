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
import zhang.lu.SimpleReader.R;

import java.util.Date;

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

		void onPercentClick();

		void onBatteryLevelClick();
	}

	public static final String DATE_FORMAT_STRING = "kk:mm";
	public static final int POPUP_WIN_BOARD_SIZE = 3 * 2;

	private OnPanelClickListener pcl;
	private View layout;
	private TextView ptv, ftv, btv, ttv;

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
		ptv = (TextView) layout.findViewById(R.id.reading_percent_text);
		ttv = (TextView) layout.findViewById(R.id.reading_time_text);
		btv = (TextView) layout.findViewById(R.id.battery_level_text);

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
				pcl.onPercentClick();
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

	public void show(String filename, String chapter, int percent, int width)
	{
		ftv.setText(filename);
		if (chapter != null)
			ptv.setText(percent + "%\n" + chapter);
		else
			ptv.setText(percent + "%");
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
}
