package zhang.lu.SimpleReader.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import org.jetbrains.annotations.Nullable;
import zhang.lu.SimpleReader.Book.BookContent;
import zhang.lu.SimpleReader.Book.PlainTextContent;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:03
 */
public abstract class SimpleTextView extends View
{
	public static interface OnPosChangeListener
	{
		void onPosChange(int pos, boolean fromUser);
	}

	public static class FingerPosInfo
	{
		public int line, offset;
		public String str;
	}

	public static final int defaultTextColor = Color.BLACK;
	public static final int defaultBackgroundColor = Color.WHITE;
	public static final int defaultNightTextColor = Color.WHITE;
	public static final int defaultNightBackgroundColor = Color.BLACK;
	public static final int defaultFontSize = 26;

	private static final BookContent defaultContent = new PlainTextContent();
	protected static BookContent content = defaultContent;
	protected static int pi = 0, po = 0;
	protected static int pos = 0;
	protected static int boardGAP = 3;
	protected static int bcolor;
	protected static OnPosChangeListener mOnPosChangeListener = null;
	protected static boolean reset = true;

	protected Paint paint;

	protected int nextpi, nextpo;
	protected int ml;

	protected float fw, fh, fd;
	protected int w, h;
	protected float xoffset, yoffset;


	public SimpleTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		paint = new Paint();
		paint.setColor(defaultTextColor);
		paint.setAntiAlias(true);
		paint.setTextSize(defaultFontSize);
		bcolor = defaultBackgroundColor;
		fontCalc();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		reset = true;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		if (reset) {
			w = getWidth();
			h = getHeight();

			resetValues();
			reset = false;
		}

		canvas.drawColor(bcolor);
		drawText(canvas);
		//testDraw(canvas);
	}

	/*
	 private void testDraw(Canvas canvas)
	 {
		 Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 textPaint.setTextSize(40);
		 textPaint.setColor(Color.BLACK);

		 // FontMetrics对象
		 Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();

		 String text = "abcdefghijklmnopqrstu计算每一个坐标";

		 // 计算每一个坐标
		 float baseX = 0;
		 float baseY = 100;
		 float topY = baseY + fontMetrics.top;
		 float ascentY = baseY + fontMetrics.ascent;
		 float descentY = baseY + fontMetrics.descent;
		 float bottomY = baseY + fontMetrics.bottom;

		 // 绘制文本
		 canvas.drawText(text, baseX, baseY, textPaint);

		 // BaseLine描画
		 Paint baseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 baseLinePaint.setColor(Color.RED);
		 canvas.drawLine(0, baseY, getWidth(), baseY, baseLinePaint);

		 // Base描画
		 canvas.drawCircle(baseX, baseY, 5, baseLinePaint);

		 // TopLine描画
		 Paint topLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 topLinePaint.setColor(Color.LTGRAY);
		 canvas.drawLine(0, topY, getWidth(), topY, topLinePaint);
		 // AscentLine描画
		 Paint ascentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 ascentLinePaint.setColor(Color.GREEN);
		 canvas.drawLine(0, ascentY, getWidth(), ascentY, ascentLinePaint);

		 // DescentLine描画
		 Paint descentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 descentLinePaint.setColor(Color.YELLOW);
		 canvas.drawLine(0, descentY, getWidth(), descentY, descentLinePaint);

		 // ButtomLine描画
		 Paint bottomLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 bottomLinePaint.setColor(Color.MAGENTA);
		 canvas.drawLine(0, bottomY, getWidth(), bottomY, bottomLinePaint);

	 }
 */
	public void setColorAndFont(int color, int aBcolor, int fontSize)
	{
		boardGAP = fontSize / 3;
		bcolor = aBcolor;
		paint.setColor(color);
		paint.setTextSize(fontSize);
		fontCalc();
		reset = true;
		//invalidate();
	}

	public void setContent(@Nullable BookContent newContent)
	{
		content.close();
		if (newContent == null)
			content = defaultContent;
		else
			content = newContent;
	}

	public int getPosIndex()
	{
		return pi;
	}

	public int getPosOffset()
	{
		return po;
	}

	public boolean pageDown()
	{
		boolean ret;
		if (ret = calcNextPos())
			invalidate();
		if ((mOnPosChangeListener != null) && calcPos())
			mOnPosChangeListener.onPosChange(pos, true);

		return ret;
	}

	public boolean pageUp()
	{
		boolean ret;
		if (ret = calcPrevPos())
			invalidate();
		if ((mOnPosChangeListener != null) && calcPos())
			mOnPosChangeListener.onPosChange(pos, true);

		return ret;
	}

	protected boolean calcNextPos()
	{
		if (nextpi >= content.getLineCount())
			return false;
		pi = nextpi;
		po = nextpo;

		return true;
	}

	protected void fontCalc()
	{
		Paint.FontMetrics fm = paint.getFontMetrics();
		fh = fm.descent - fm.ascent;
		fw = paint.measureText("漢", 0, 1);
		fd = fm.descent;
	}

	public int getPos()
	{
		calcPos();
		return pos;
	}

	public void setPos(int np)
	{
		if (content.size() == 0)
			return;

		setPos(content.getPercentPos(np));
		pos = np;
		if (mOnPosChangeListener != null)
			mOnPosChangeListener.onPosChange(pos, false);
	}

	public void setPos(BookContent.ContentPosInfo cpi)
	{
		setPos(cpi.line, cpi.offset);
	}

	public void setPos(int posIndex, int posOffset)
	{
		if (content.size() == 0)
			return;

		pos = 0;

		if (posIndex >= content.getLineCount()) {
			pi = po = 0;
		} else if (posOffset >= content.line(posIndex).length()) {
			pi = posIndex;
			po = 0;
		} else {
			pi = posIndex;
			po = calcPosOffset(posOffset);
		}

		if (mOnPosChangeListener != null) {
			calcPos();
			mOnPosChangeListener.onPosChange(pos, false);
		}
	}

	public void setOnPosChangeListener(@Nullable OnPosChangeListener listener)
	{
		mOnPosChangeListener = listener;
		if (mOnPosChangeListener == null) {
			pos = 0;
			return;
		}
		calcPos();
		mOnPosChangeListener.onPosChange(pos, false);
	}

	protected boolean calcPos()
	{
		int s = content.size();
		if (s == 0)
			return false;

		int p = content.size(pi) + po;

		int np = p * 100 / s;
		if (pos != np) {
			pos = np;
			return true;
		}
		return false;
	}

	public FingerPosInfo getFingerPosInfo(float x, float y)
	{
		FingerPosInfo pi = calcFingerPos(x, y);
		if (pi == null)
			return null;
		String l = content.line(pi.line);
		if (pi.offset >= l.length())
			return null;
		pi.str = l.substring(pi.offset);
		return pi;
	}

	public String getFingerPosNote(float x, float y)
	{
		FingerPosInfo pi = calcFingerPos(x, y);
		if (pi == null)
			return null;

		return content.getNote(pi.line, pi.offset);
	}

	public BookContent.ContentPosInfo searchText(String t)
	{
		if (t == null)
			return null;
		if (t.length() == 0)
			return null;

		BookContent.ContentPosInfo sr = new BookContent.ContentPosInfo();
		sr.offset = calcNextLineOffset();
		sr.line = getPosIndex();
		if (sr.offset == -1) {
			sr.line++;
			sr.offset = 0;
		}

		return content.searchText(t, sr);
	}

	protected String replaceTextChar(char[] txt, char[] oc, char[] nc)
	{
		for (int i = 0; i < oc.length; i++)
			for (int j = 0; j < txt.length; j++)
				if (txt[j] == oc[i])
					txt[j] = nc[i];
		return String.valueOf(txt);
//		return txt.replace('「', '﹁').replace('」', '﹂').replace('『', '﹃').replace('』', '﹄').replace('（', '︵')
//			  .replace('）', '︶').replace('《', '︽').replace('》', '︾').replace('〔', '︹').replace('〕', '︺')
//			  .replace('【', '︻').replace('】', '︼').replace('｛', '︷').replace('｝', '︸').replace('─', '︱');
	}

	public abstract int calcNextLineOffset();

	protected abstract int calcPosOffset(int npo);

	protected abstract void resetValues();

	protected abstract boolean calcPrevPos();

	protected abstract void drawText(Canvas canvas);

	protected abstract FingerPosInfo calcFingerPos(float x, float y);
}
