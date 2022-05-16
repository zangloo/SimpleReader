package net.lzrj.SimpleReader.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.ContentLineType;
import net.lzrj.SimpleReader.ImageContent;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.Content;
import net.lzrj.SimpleReader.book.ContentBase;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:03
 */
public abstract class SimpleTextView extends View
{
	public enum TapTargetType
	{
		text, image, none
	}

	public static class TapTarget
	{
		public final int line, offset;
		public String str;
		public TapTargetType type;
		public int x, y;

		public TapTarget(int line, int offset)
		{
			this.line = line;
			this.offset = offset;
			this.type = TapTargetType.text;
		}
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

	protected static class FontMeasure
	{
		protected float width;
		protected float height;
		protected float descent;
	}

	protected static class DrawChar
	{
		int offset;
		int fontSize;
		Rect rect;

		public DrawChar(int offset, int fontSize, Rect rect)
		{
			this.offset = offset;
			this.fontSize = fontSize;
			this.rect = rect;
		}
	}

	protected static class DrawLine
	{
		final int line;
		float drawSize;
		float space;
		final List<DrawChar> chars = new ArrayList<>();

		public DrawLine(int line, float drawSize, float space)
		{
			this.line = line;
			this.drawSize = drawSize;
			this.space = space;
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
	protected static int percent = 0;
	protected static int viewMargin = 3;
	protected static int backgroundColor, color;
	protected static boolean reset = true;
	protected static HighlightInfo highlightInfo = null;

	protected Paint paint;

	protected static Content.Position current = new Content.Position(0, 0);
	protected Content.Position next;

	private final Map<Integer, FontMeasure> fontMeasureMap = new Hashtable<>();
	protected final List<DrawLine> drawLines = new ArrayList<>();
	protected int fontSize;
	protected int pageWidth, pageHeight;

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
		backgroundColor = defaultBackgroundColor;
		color = defaultTextColor;
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
			reset = false;
		}

		canvas.drawColor(backgroundColor);
		if (current.line >= content.lineCount())
			return;
		drawText(canvas);
		//testDraw(canvas);
	}

	protected void drawImage(Canvas canvas, ImageContent image)
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
		ContentLine line = content.line(current.line);
		if (ContentLineType.image.equals(line.type()))
			return ((ImageContent) line).getImage();
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
		this.fontSize = fontSize;
		backgroundColor = aBcolor;
		color = aColor;
		paint.setColor(color);
		paint.setTypeface(typeface);
		setTextSize(paint, fontSize);
		FontMeasure fontMeasure = fontMeasure(fontSize);
		viewMargin = (int) (fontMeasure.height / 2);
		reset = true;
		//invalidate();
	}

	protected void setTextSize(Paint paint, int dip)
	{
		float px = dip * (metrics.densityDpi / 160f);
		paint.setStrokeWidth(px / 15);
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
		return current.line;
	}

	public int getPosOffset()
	{
		return current.offset;
	}

	public boolean pageDown()
	{
		if (!nextPage())
			return false;

		invalidate();
		return true;
	}

	public boolean pageUp()
	{
		if ((current.line == 0) && (current.offset == 0))
			return false;

		if (!gotoPrevPage())
			return false;

		invalidate();
		return true;
	}

	protected boolean nextPage()
	{
		if (next == null)
			return false;
		current = next;
		return true;
	}

	protected FontMeasure fontCalc()
	{
		Paint.FontMetrics fm = paint.getFontMetrics();
		FontMeasure fontMeasure = new FontMeasure();
		fontMeasure.height = fm.descent - fm.ascent;
		fontMeasure.width = paint.measureText("漢", 0, 1);
		fontMeasure.descent = fm.descent;
		return fontMeasure;
	}

	public int getPercent()
	{
		updatePercent();
		return percent;
	}

	public void setPercent(int targetPercent)
	{
		if (content.size() == 0)
			return;

		setPercent(content.getPercentPos(targetPercent));
		percent = targetPercent;
	}

	public void setPercent(Content.Position cpi)
	{
		setPercent(cpi.line, cpi.offset);
	}

	public void setPercent(int posIndex, int posOffset)
	{
		percent = 0;
		if (content.size() == 0) {
			current.line = current.offset = 0;
			return;
		}

		if (posIndex >= content.lineCount()) {
			current.line = current.offset = 0;
		} else {
			int offset = calcPercentOffset(posIndex, posOffset);
			current.line = posIndex;
			current.offset = offset;
		}
	}

	private int calcPercentOffset(int line, int offset)
	{
		ContentLine contentLine = content.line(line);
		int length = contentLine.length();
		if (contentLine instanceof UString && ((UString) contentLine).isParagraph())
			length += 2;
		if (!(contentLine instanceof UString) || offset >= length)
			return 0;
		List<DrawLine> wrapLines = wrapLine(line, (UString) contentLine, 0, contentLine.length(), createDrawContext());
		for (DrawLine wrapLine : wrapLines) {
			List<DrawChar> chars = wrapLine.chars;
			int charCount = chars.size();
			if (charCount == 0)
				return 0;
			if (offset < chars.get(charCount - 1).offset)
				return chars.get(0).offset;
		}
		return 0;
	}

	protected void updatePercent()
	{
		int s = content.size();
		if (s == 0)
			return;

		int p = content.size(current.line) + current.offset;
		percent = p * 100 / s;
	}

	public TapTarget getTapTarget(float x, float y)
	{
		TapTarget fpi;
		if (content.line(current.line).isImage()) {
			fpi = new TapTarget(current.line, 0);
			fpi.type = TapTargetType.image;
			return fpi;
		}

		fpi = calcTapTarget(x, y);
		if (fpi == null) {
			fpi = new TapTarget(0, 0);
			fpi.type = TapTargetType.none;
			return fpi;
		}
		fpi.type = TapTargetType.text;
		UString l = content.text(fpi.line);
		if (fpi.offset >= l.length())
			return null;
		fpi.str = l.substring(fpi.offset);
		return fpi;
	}

	public String getTapTargetNote(float x, float y)
	{
		if (content.line(current.line).isImage())
			return null;

		if (!content.hasNotes())
			return null;
		TapTarget pi = calcTapTarget(x, y);
		if (pi == null)
			return null;

		return content.getNote(pi.line, pi.offset);
	}

	public Content.Position searchText(String t)
	{
		ContentLine contentLine = content.line(current.line);
		if (contentLine.isImage())
			return null;
		if (t == null)
			return null;
		if (t.length() == 0)
			return null;

		List<DrawLine> wrapLines = wrapLine(current.line, (UString) contentLine, current.offset, contentLine.length(), createDrawContext());
		Content.Position position = new Content.Position();
		if (wrapLines.size() <= 1) {
			position.line = current.line + 1;
			position.offset = 0;
		} else {
			position.line = current.line;
			List<DrawChar> chars = wrapLines.get(1).chars;
			position.offset = chars.get(0).offset;
		}
		return content.searchText(t, position);
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
		highlightInfo = hightlightInfo;
	}

	public void gotoEnd()
	{
		current.line = content.lineCount();
		current.offset = 0;
		gotoPrevPage();
	}

	public boolean isImagePage()
	{
		return ContentLineType.image.equals(content.line(current.line).type());
	}

	protected static class DrawContext
	{
		final int maxDrawSize;
		final int maxLineSize;
		int baseline;

		public DrawContext(int maxDrawSize, int maxLineSize, int baseline)
		{
			this.maxDrawSize = maxDrawSize;
			this.maxLineSize = maxLineSize;
			this.baseline = baseline;
		}
	}

	protected void drawText(Canvas canvas)
	{
		int lineCount = content.lineCount();
		drawLines.clear();
		float drawnSize = 0;
		int offset = current.offset;
		DrawContext drawContext = createDrawContext();
		next = null;
		Map<Integer, UString> preparedLines = new HashMap<>();

		OUTER:
		for (int i = current.line; i < lineCount; i++) {
			ContentLine contentLine = content.line(i);
			if (contentLine.isImage()) {
				if (current.line == i) {
					drawImage(canvas, (ImageContent) contentLine);
					int nextLine = i + 1;
					if (nextLine < lineCount)
						next = new Content.Position(nextLine, 0);
					return;
				} else {
					next = new Content.Position(i, 0);
					break;
				}
			}

			UString line = prepareLineForDraw((UString) contentLine);
			preparedLines.put(i, line);
			List<DrawLine> wrapLines = wrapLine(i, line, offset, line.length(), drawContext);
			offset = 0;
			for (DrawLine wrapLine : wrapLines) {
				float newSize = drawnSize + wrapLine.drawSize;
				if (newSize > drawContext.maxDrawSize) {
					List<DrawChar> chars = wrapLine.chars;
					next = new Content.Position(wrapLine.line, chars.size() == 0 ? 0 : chars.get(0).offset);
					break OUTER;
				}
				drawLines.add(wrapLine);
				drawnSize = newSize + wrapLine.space;
			}
		}
		char[] buf = new char[2];
		int fontSize = 0;
		FontMeasure fontMeasure = null;
		for (DrawLine drawLine : drawLines) {
			int line = drawLine.line;
			UString text = preparedLines.get(line);
			for (DrawChar drawChar : drawLine.chars) {
				offset = drawChar.offset;
				boolean highlight = ((highlightInfo != null) && (highlightInfo.line == line) && (highlightInfo.end > offset) && (highlightInfo.begin <= offset));
				Rect rect = drawChar.rect;
				if (highlight) {
					canvas.drawRect(rect, paint);
					paint.setColor(backgroundColor);
				}
				int ch = text.charAt(offset);
				int charWidth = Character.toChars(ch, buf, 0);
				if (fontMeasure == null || fontSize != drawChar.fontSize) {
					fontSize = drawChar.fontSize;
					setTextSize(paint, fontSize);
					fontMeasure = fontMeasure(fontSize);
				}
//				canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
//				canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, paint);
//				canvas.drawLine(rect.right, rect.bottom, rect.right, rect.top, paint);
//				canvas.drawLine(rect.right, rect.bottom, rect.left, rect.bottom, paint);
				canvas.drawText(buf, 0, charWidth, rect.left, rect.bottom - fontMeasure.descent, paint);
				if (highlight)
					paint.setColor(color);
			}
		}
	}

	protected TapTarget calcTapTarget(float x, float y)
	{
		for (DrawLine drawLine : drawLines)
			for (DrawChar drawChar : drawLine.chars)
				if (drawChar.rect.contains((int) x, (int) y))
					return new TapTarget(drawLine.line, drawChar.offset);
		return null;
	}

	protected boolean gotoPrevPage()
	{
		int i, offset;
		ContentLine contentLine;
		if (current.offset == 0) {
			if (current.line == 0)
				return false;
			i = current.line - 1;
			contentLine = content.line(i);
			offset = contentLine.length();
		} else {
			i = current.line;
			contentLine = content.line(i);
			offset = current.offset;
		}
		DrawContext drawContext = createDrawContext();
		float totalSize = 0;
		while (true) {
			if (contentLine.isImage()) {
				if (totalSize == 0)
					current.line = i;
				else
					current.line = i + 1;
				return true;
			}
			List<DrawLine> wrapLines = wrapLine(i, (UString) contentLine, 0, offset, drawContext);
			int wrappedLines = wrapLines.size() - 1;
			for (int wi = wrappedLines; wi >= 0; wi--) {
				DrawLine wrapLine = wrapLines.get(wi);
				float newTotalSize;
				if (totalSize == 0)
					newTotalSize = wrapLine.drawSize;
				else
					newTotalSize = totalSize + wrapLine.drawSize + wrapLine.space;
				if (newTotalSize > drawContext.maxDrawSize) {
					if (wi == wrappedLines) {
						current.line = wrapLine.line + 1;
						current.offset = 0;
					} else {
						current.line = wrapLine.line;
						List<DrawChar> chars = wrapLine.chars;
						int charCount = chars.size();
						current.offset = charCount == 0 ? 0 : chars.get(charCount - 1).offset + 1;
					}
					return true;
				}
				totalSize = newTotalSize;
			}
			if (i <= 0)
				break;
			i--;
			contentLine = content.line(i);
			offset = contentLine.length();
		}
		current.line = 0;
		current.offset = 0;
		return true;
	}

	protected FontMeasure fontMeasure(int fontSize)
	{
		FontMeasure measure = fontMeasureMap.get(fontSize);
		if (measure != null)
			return measure;
		setTextSize(paint, fontSize);
		measure = fontCalc();
		fontMeasureMap.put(fontSize, measure);
		return measure;
	}

	protected float measureChar(int ch)
	{
		char[] buf = new char[2];
		int charWidth = Character.toChars(ch, buf, 0);
		return paint.measureText(buf, 0, charWidth);
	}

	protected int scaleFontSize(int sizeDelta)
	{
		return this.fontSize + (sizeDelta * 6);
	}

	protected abstract DrawContext createDrawContext();

	protected abstract UString prepareLineForDraw(UString line);

	protected abstract List<DrawLine> wrapLine(int line, UString text, int begin, int end, DrawContext drawContext);

	protected abstract void drawUnderline(UString line, int charFrom, int charTo, float x, float y, float fontHeight, float fontDescent, Canvas canvas, Paint paint);
}
