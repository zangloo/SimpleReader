package zhang.lu.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-25
 * Time: 下午8:56
 */
public class HNoteView extends TextView
{
	float fh, fw, fd;
	float xoffset, yoffset;

	public HNoteView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		calcFont(getPaint());
	}

	@Override
	public void setTypeface(Typeface tf, int style)
	{
		super.setTypeface(tf, style);
		calcFont(getPaint());
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		Drawable d = getBackground();
		if (d != null)
			d.draw(canvas);
		Paint p = getPaint();
		p.setColor(getTextColors().getDefaultColor());
		char[] str = getText().toString().toCharArray();
		float[] xy = new float[str.length * 2];
		int h = getHeight();
		float x = getWidth() - xoffset - fw;
		float y = yoffset + fh;
		for (int i = 0; i < str.length; i++) {
			xy[i * 2] = x;
			xy[i * 2 + 1] = y;
			y += fh;
			if (y > h) {
				y = yoffset + fh;
				x -= fw;
			}
		}
		SimpleTextView.replaceTextChar(str, HTextView.SC, HTextView.TC);
		canvas.drawPosText(str, 0, str.length, xy, p);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int wm = MeasureSpec.getMode(widthMeasureSpec);
		int hm = MeasureSpec.getMode(heightMeasureSpec);
		int ws = MeasureSpec.getSize(widthMeasureSpec);
		int hs = MeasureSpec.getSize(heightMeasureSpec);
		float w, h;
		int c = getText().length();
		switch (hm) {
			case MeasureSpec.EXACTLY:
				h = hs;
				break;
			case MeasureSpec.AT_MOST:
				h = Math.min(hs, c * fh);
				break;
			case MeasureSpec.UNSPECIFIED:
			default:
				h = fh * c;
				break;
		}
		int lc = (int) (Math.max(h, hs) / fh);
		switch (wm) {
			case MeasureSpec.EXACTLY:
				w = ws;
				break;
			case MeasureSpec.AT_MOST:
				w = Math.min(((c + lc - 1) / lc) * fw, ws);
				break;
			case MeasureSpec.UNSPECIFIED:
			default:
				w = ((c + lc - 1) / lc) * fw;
				break;
		}
		// +1 for float to int will lost a little, and reenter this function will cause some problem
		setMeasuredDimension((int) w + 1, (int) h + 1);
	}

	private void calcFont(Paint paint)
	{
		Paint.FontMetrics fm = paint.getFontMetrics();
		fh = fm.descent - fm.ascent;
		fw = paint.measureText("漢", 0, 1);
		fd = fm.descent;
		xoffset = 0;
		yoffset = -fd + 1;
	}
}
