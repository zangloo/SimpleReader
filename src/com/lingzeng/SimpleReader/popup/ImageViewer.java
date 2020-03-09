package com.lingzeng.SimpleReader.popup;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import com.lingzeng.SimpleReader.R;
import com.lingzeng.SimpleReader.view.TouchImageView;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-3-24
 * Time: 上午11:35
 */
public class ImageViewer extends PopupWindow implements PopupWindow.OnDismissListener
{
	private View layout;
	private TouchImageView iv;

	public ImageViewer(Context context)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.imageviewer, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(WindowManager.LayoutParams.FILL_PARENT);
		setWidth(WindowManager.LayoutParams.FILL_PARENT);

		Button btn = (Button) layout.findViewById(R.id.zoom_restore);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				ImageViewer.this.hide();
			}
		});

		btn = (Button) layout.findViewById(R.id.zoom_in);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				iv.zoomIn();
			}
		});

		btn = (Button) layout.findViewById(R.id.zoom_out);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				iv.zoomOut();
			}
		});

		setOnDismissListener(this);
		iv = (TouchImageView) layout.findViewById(R.id.image_view);
		iv.setMaxZoom(4f);
	}

	public void show(Bitmap bm)
	{
		iv.setImageBitmap(bm);
		showAtLocation(layout, Gravity.NO_GRAVITY, 0, 0);
	}

	public void hide()
	{
		dismiss();
	}

	@Override
	public void onDismiss()
	{
		iv.setImageBitmap(null);
	}
}
