package zhang.lu.SimpleReader.popup;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.R;

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

		void onColorButtonClick();
	}

	private OnPanelClickListener pcl;
	private View layout;
	private TextView ptv, ftv, ctv;
	private Button pbt, nbt;
	private Stack<Config.ReadingInfo> ris;
	private Stack<Config.ReadingInfo> nis = new Stack<Config.ReadingInfo>();
	private Config.ReadingInfo cri;

	public StatusPanel(Context context, OnPanelClickListener onPanelClickListener)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.statuspanel, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		setWidth(WindowManager.LayoutParams.FILL_PARENT);

		pcl = onPanelClickListener;

		ftv = (TextView) layout.findViewById(R.id.reading_book_text);
		ctv = (TextView) layout.findViewById(R.id.reading_chapter_text);
		ptv = (TextView) layout.findViewById(R.id.reading_percent_text);

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
				pcl.onColorButtonClick();
			}
		});
	}

	public void show(Stack<Config.ReadingInfo> ris, Config.ReadingInfo ri)
	{
		this.ris = ris;
		nis.clear();
		cri = ri;
		pbt.setEnabled(ris.size() > 0);
		nbt.setEnabled(false);

		updateReadingInfo(ri);
		showAtLocation(layout, Gravity.TOP | Gravity.CENTER, 0, 0);
	}

	public void hide()
	{
		dismiss();
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
