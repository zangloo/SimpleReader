package zhang.lu.SimpleReader.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import org.jetbrains.annotations.Nullable;
import zhang.lu.SimpleReader.book.Content;
import zhang.lu.SimpleReader.book.PlainTextContent;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:03
 */
public abstract class SimpleTextView extends View
{
	public static class FingerPosInfo
	{
		public int line, offset;
		public String str;
	}

	public static class HighlightInfo
	{
		public int line;
		public int begin, end;

		public HighlightInfo(int l, int b, int e)
		{
			line = l;
			begin = b;
			end = e;
		}
	}

	public static final int defaultTextColor = Color.BLACK;
	public static final int defaultBackgroundColor = Color.WHITE;
	public static final int defaultNightTextColor = Color.WHITE;
	public static final int defaultNightBackgroundColor = Color.BLACK;
	public static final int defaultFontSize = 26;

	private static final Content defaultContent = new PlainTextContent();
	protected static Content content = defaultContent;
	protected static int pi = 0, po = 0;
	protected static int pos = 0;
	protected static int boardGAP = 3;
	protected static int bcolor, color;
	protected static boolean reset = true;
	protected static HighlightInfo hli = null;

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
		color = defaultTextColor;
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
		if (pi < content.imageCount()) {
			drawImage(canvas);
			return;
		}
		if (pi >= content.lineCount())
			return;
		drawText(canvas);
		//testDraw(canvas);
	}

	private void drawImage(Canvas canvas)
	{
		if (pi >= content.imageCount())
			return;
		Bitmap bm = content.image(pi);
		if (bm == null)
			return;
		canvas.drawBitmap(bm, null, new Rect(0, 0, w, h), null);
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
	public void setColorAndFont(int aColor, int aBcolor, int fontSize, Typeface typeface)
	{
		boardGAP = fontSize / 3;
		bcolor = aBcolor;
		color = aColor;
		paint.setColor(color);
		paint.setTextSize(fontSize);
		paint.setTypeface(typeface);
		fontCalc();
		reset = true;
		//invalidate();
	}

	public void setContent(@Nullable Content newContent)
	{
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
		if (pi < content.imageCount() - 1)
			pi++;
		else if (pi == content.imageCount() - 1)
			if (content.lineCount() <= content.imageCount())
				return false;
			else
				pi++;
		else if (!calcNextPos())
			return false;

		invalidate();
		return true;
	}

	public boolean pageUp()
	{
		if ((pi == 0) && (po == 0))
			return false;

		if (pi < content.imageCount())
			if (pi > 0)
				pi--;
			else
				return false;
		else if ((pi == content.imageCount()) && (po == 0))
			pi--;
		else if (!calcPrevPos())
			return false;
		else if (pi < content.imageCount()) {
			pi = content.imageCount();
			po = 0;
		}

		invalidate();
		return true;
	}

	protected boolean calcNextPos()
	{
		if (nextpi >= content.lineCount())
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
	}

	public void setPos(Content.ContentPosInfo cpi)
	{
		setPos(cpi.line, cpi.offset);
	}

	public void setPos(int posIndex, int posOffset)
	{
		if (content.size() == 0)
			return;

		pos = 0;

		if (posIndex >= content.lineCount()) {
			pi = po = 0;
		} else if (posOffset >= content.line(posIndex).length()) {
			pi = posIndex;
			po = 0;
		} else {
			pi = posIndex;
			po = calcPosOffset(posOffset);
		}
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
		if (pi < content.imageCount())
			return null;

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
		if (pi < content.imageCount())
			return null;

		if (!content.hasNotes())
			return null;
		FingerPosInfo pi = calcFingerPos(x, y);
		if (pi == null)
			return null;

		return content.getNote(pi.line, pi.offset);
	}

	public Content.ContentPosInfo searchText(String t)
	{
		if (pi < content.imageCount())
			return null;
		if (t == null)
			return null;
		if (t.length() == 0)
			return null;

		Content.ContentPosInfo sr = new Content.ContentPosInfo();
		sr.offset = calcNextLineOffset();
		sr.line = getPosIndex();
		if (sr.offset == -1) {
			sr.line++;
			sr.offset = 0;
		}

		return content.searchText(t, sr);
	}

	protected static String replaceTextChar(char[] txt, char[] oc, char[] nc)
	{
		for (int i = 0; i < oc.length; i++)
			for (int j = 0; j < txt.length; j++)
				if (txt[j] == oc[i])
					txt[j] = nc[i];
		return String.valueOf(txt);
	}

	public void setHighlightInfo(@Nullable HighlightInfo hightlightInfo)
	{
		hli = hightlightInfo;
	}

	public void gotoEnd()
	{
		pi = content.lineCount();
		po = 0;
		calcPrevPos();

		if (pi >= content.imageCount())
			return;
		po = 0;
		if (content.lineCount() > content.imageCount())
			pi = content.imageCount();
		else
			pi = content.imageCount() - 1;
	}

	protected abstract int calcNextLineOffset();

	protected abstract int calcPosOffset(int npo);

	protected abstract void resetValues();

	protected abstract boolean calcPrevPos();

	protected abstract void drawText(Canvas canvas);

	protected abstract FingerPosInfo calcFingerPos(float x, float y);
}
