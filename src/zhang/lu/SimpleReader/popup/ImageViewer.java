package zhang.lu.SimpleReader.popup;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import zhang.lu.SimpleReader.R;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-3-24
 * Time: 上午11:35
 */
public class ImageViewer extends PopupWindow
{
	public static int CENT_SCALE = 1000;
	private View layout;
	private ImageView iv;
	private float ox, oy;
	private int bx, by;
	private int w, h, vw = -1, vh;

	public ImageViewer(Context context)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = inflater.inflate(R.layout.imageviewer, null, true);

		setContentView(layout);
		setFocusable(true);
		setHeight(WindowManager.LayoutParams.FILL_PARENT);
		setWidth(WindowManager.LayoutParams.FILL_PARENT);

		Button btn = (Button) layout.findViewById(R.id.button_ok);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				ImageViewer.this.hide();
			}
		});

		iv = (ImageView) layout.findViewById(R.id.image_view);
		iv.setOnTouchListener(new View.OnTouchListener()
		{
			public boolean onTouch(View arg0, MotionEvent event)
			{
				float nx, ny;

				if (vw == -1) {
					vw = iv.getWidth();
					vh = iv.getHeight();
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						ox = event.getX();
						oy = event.getY();
						break;
					case MotionEvent.ACTION_MOVE:
					case MotionEvent.ACTION_UP:
						nx = event.getX();
						ny = event.getY();
						bx = check(bx + (int) (ox - nx), (w - vw) / 2);
						by = check(by + (int) (oy - ny), (h - vh) / 2);
						iv.scrollTo(bx, by);
						ox = nx;
						oy = ny;
						break;
				}
				return true;
			}
		});
	}

	private int check(int v, int mv)
	{
		if (mv < 0) return 0;
		if (v < (-mv)) return -mv;
		if (v > mv) return mv;
		return v;
	}

	public void show(Bitmap bm, int x, int y)
	{
		iv.setImageBitmap(bm);
		vw = -1;
		w = bm.getWidth();
		h = bm.getHeight();
		bx = w * x / CENT_SCALE - w / 2;
		by = h * y / CENT_SCALE - h / 2;
		iv.scrollTo(bx, by);
		showAtLocation(layout, Gravity.NO_GRAVITY, 0, 0);
	}

	@Override
	public void update(int width, int height)
	{
		super.update(width, height);
		vw = -1;
	}

	public void hide()
	{
		dismiss();
	}

}
