package com.lingzeng.SimpleReader.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import com.lingzeng.SimpleReader.ContentImage;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.ContentLineType;
import org.jetbrains.annotations.Nullable;
import com.lingzeng.SimpleReader.UString;
import com.lingzeng.SimpleReader.book.Content;
import com.lingzeng.SimpleReader.book.ContentBase;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:03
 */
public abstract class SimpleTextView extends View
{
	public static enum FingerPosType
	{
		text, image, none
	}

	public static class FingerPosInfo
	{
		public int line, offset;
		public String str;
		public FingerPosType type;
		public int x, y;
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
	public static final int zoomIconSize = 48;

	private static final Content defaultContent = new ContentBase();
	protected static Content content = defaultContent;
	protected static int pi = 0, po = 0;
	protected static int pos = 0;
	protected static int boardGAP = 3;
	protected static int bcolor, color;
	protected static boolean reset = true;
	protected static HighlightInfo hli = null;

	protected Paint paint;

	protected int nextpi, nextpo;
	protected int maxLinePerPage;

	protected float fontWidth, fontHeight, fontDescent;
	protected int pageWidth, pageHeight;
	protected float xoffset, yoffset;

	private static Bitmap zoomIcon = null;
	DisplayMetrics metrics;

	public SimpleTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		metrics = getContext().getResources().getDisplayMetrics();
		paint = new Paint();
		paint.setColor(defaultTextColor);
		paint.setAntiAlias(true);
		setTextSize(paint, defaultFontSize);
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
			pageWidth = getWidth();
			pageHeight = getHeight();

			resetValues();
			reset = false;
		}

		canvas.drawColor(bcolor);
		if (pi >= content.lineCount())
			return;
		drawText(canvas);
		//testDraw(canvas);
	}

	protected void drawImage(Canvas canvas, ContentImage image)
	{
		Bitmap b = image.getImage();
		if (b == null)
			return;
		canvas.drawBitmap(b, null, new Rect(0, 0, pageWidth, pageHeight), null);
		if (zoomIcon != null) {
			canvas.drawBitmap(zoomIcon, null,
				new Rect(pageWidth - zoomIconSize, pageHeight - zoomIconSize, pageWidth, pageHeight),
				null);
		}
	}

	public Bitmap getImage()
	{
		ContentLine line = content.line(pi);
		if (ContentLineType.image.equals(line.type()))
			return ((ContentImage) line).getImage();
		else
			return null;
	}

	public static void setZoomIcon(Bitmap icon)
	{
		zoomIcon = icon;
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
		setTextSize(paint, fontSize);
		paint.setTypeface(typeface);
		fontCalc();
		reset = true;
		//invalidate();
	}

	private void setTextSize(Paint paint, int dip)
	{
		float px = dip * (metrics.densityDpi / 160f);
		paint.setTextSize(px);
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
		if (!calcNextPos())
			return false;

		invalidate();
		return true;
	}

	public boolean pageUp()
	{
		if ((pi == 0) && (po == 0))
			return false;

		if (!calcPrevPos())
			return false;

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
		fontHeight = fm.descent - fm.ascent;
		fontWidth = paint.measureText("漢", 0, 1);
		fontDescent = fm.descent;
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
		FingerPosInfo fpi;
		if (content.line(pi).isImage()) {
			fpi = new FingerPosInfo();
			fpi.type = FingerPosType.image;
			fpi.line = pi;
			return fpi;
		}

		fpi = calcFingerPos(x, y);
		if (fpi == null) {
			fpi = new FingerPosInfo();
			fpi.type = FingerPosType.none;
			return fpi;
		}
		fpi.type = FingerPosType.text;
		UString l = content.text(fpi.line);
		if (fpi.offset >= l.length())
			return null;
		fpi.str = l.substring(fpi.offset);
		return fpi;
	}

	public String getFingerPosNote(float x, float y)
	{
		if (content.line(pi).isImage())
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
		if (content.line(pi).isImage())
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

	public static void replaceTextChar(char[] txt, char[] oc, char[] nc)
	{
		for (int i = 0; i < oc.length; i++)
			for (int j = 0; j < txt.length; j++)
				if (txt[j] == oc[i])
					txt[j] = nc[i];
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
	}

	public boolean isImagePage()
	{
		return ContentLineType.image.equals(content.line(pi).type());
	}

	protected abstract int calcNextLineOffset();

	protected abstract int calcPosOffset(int npo);

	protected abstract void resetValues();

	protected abstract boolean calcPrevPos();

	protected abstract void drawText(Canvas canvas);

	protected abstract FingerPosInfo calcFingerPos(float x, float y);
}
