package net.lzrj.SimpleReader.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import net.lzrj.SimpleReader.*;
import net.lzrj.SimpleReader.book.Content;
import net.lzrj.SimpleReader.book.ContentBase;
import net.lzrj.SimpleReader.book.TextStyleType;
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
		final int offset;
		final int fontSize;
		final Rect rect;
		final Point drawOffset;
		final Integer color;
		final Integer background;
		final Bitmap image;

		public DrawChar(int offset, int fontSize, Rect rect, Point drawOffset, Integer color, Integer background, Bitmap image)
		{
			this.offset = offset;
			this.fontSize = fontSize;
			this.rect = rect;
			this.drawOffset = drawOffset;
			this.color = color;
			this.background = background;
			this.image = image;
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
		UString contentLine = content.line(line);
		int length = contentLine.length();
		if (contentLine.isParagraph())
			length += 2;
		if (offset >= length)
			return 0;
		List<DrawLine> wrapLines = wrapLine(line, contentLine, 0, contentLine.length(), createDrawContext());
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
		TapTarget fpi = calcTapTarget(x, y);
		if (fpi == null) {
			fpi = new TapTarget(0, 0);
			fpi.type = TapTargetType.none;
			return fpi;
		}
		fpi.type = TapTargetType.text;
		UString l = content.line(fpi.line);
		if (fpi.offset >= l.length())
			return null;
		fpi.str = l.substring(fpi.offset);
		return fpi;
	}

	public String getNote(int line, int offset)
	{
		return content.getNote(line, offset);
	}

	public String getLink(int line, int offset)
	{
		UString text = content.line(line);
		for (UString.TextStyle style : text.styles())
			if (TextStyleType.link.equals(style.type) && style.from <= offset && style.to > offset)
				return (String) style.value;
		return null;
	}

	public Content.Position searchText(String t)
	{
		UString contentLine = content.line(current.line);
		if (t == null)
			return null;
		if (t.length() == 0)
			return null;

		List<DrawLine> wrapLines = wrapLine(current.line, contentLine, current.offset, contentLine.length(), createDrawContext());
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
			UString line = prepareLineForDraw(content.line(i));
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
			for (DrawChar drawChar : drawLine.chars)
				if (drawChar.image == null) {
					offset = drawChar.offset;
					boolean highlight = ((highlightInfo != null) && (highlightInfo.line == line) && (highlightInfo.end > offset) && (highlightInfo.begin <= offset));
					Rect rect = drawChar.rect;
					if (highlight) {
						paint.setColor(color);
						canvas.drawRect(rect, paint);
						paint.setColor(backgroundColor);
					} else {
						if (drawChar.background != null) {
							paint.setColor(drawChar.background);
							canvas.drawRect(rect, paint);
						}
						if (drawChar.color == null)
							paint.setColor(color);
						else
							paint.setColor(drawChar.color);
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
					int left = rect.left;
					int bottom = rect.bottom;
					Point drawOffset = drawChar.drawOffset;
					if (drawOffset != null) {
						left += drawOffset.x;
						bottom -= drawOffset.y;
					}
					canvas.drawText(buf, 0, charWidth, left, bottom - fontMeasure.descent, paint);
				} else {
					canvas.drawBitmap(drawChar.image, null, drawChar.rect, null);
				}
			drawStyles(text, drawLine, canvas, paint);
		}
	}

	public TapTarget calcTapTarget(float x, float y)
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
		UString contentLine;
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
			List<DrawLine> wrapLines = wrapLine(i, contentLine, 0, offset, drawContext);
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

	protected int scaleFontSize(int fontSize)
	{
		if (fontSize == 100)
			return this.fontSize;
		else
			return this.fontSize * fontSize / 100;
	}

	protected void drawStyles(UString text, DrawLine drawText, Canvas canvas, Paint paint)
	{
		List<DrawChar> chars = drawText.chars;
		if (chars.size() == 0)
			return;
		List<UString.TextStyle> styles = text.styles();
		if (styles == null)
			return;
		int drawTextFrom = chars.get(0).offset;
		int drawTextTo = chars.get(chars.size() - 1).offset + 1;
		for (UString.TextStyle style : styles) {
			if (TextStyleType.fontSize.equals(style.type)
				|| TextStyleType.image.equals(style.type)
				|| TextStyleType.color.equals(style.type)
				|| TextStyleType.background.equals(style.type))
				continue;
			int from = style.from;
			int to = style.to;
			// style draw from -> to
			int styleFrom, styleTo;
			// is style fully draw
			boolean fromStart, toEnd;
			if (from >= drawTextFrom && from < drawTextTo) {
				styleFrom = from;
				fromStart = true;
				if (to > drawTextTo) {
					styleTo = drawTextTo;
					toEnd = false;
				} else {
					styleTo = to;
					toEnd = true;
				}
			} else if (to > drawTextFrom && to <= drawTextTo) {
				styleTo = to;
				toEnd = true;
				if (from < drawTextFrom) {
					styleFrom = drawTextFrom;
					fromStart = false;
				} else {
					styleFrom = from;
					fromStart = true;
				}
			} else if (from < drawTextFrom && to > drawTextTo) {
				styleFrom = drawTextFrom;
				styleTo = drawTextTo;
				fromStart = false;
				toEnd = false;
			} else
				continue;

			int first = styleFrom - drawTextFrom;
			DrawChar firstDrawChar = chars.get(first);
			if (firstDrawChar.color == null)
				paint.setColor(color);
			else
				paint.setColor(firstDrawChar.color);
			drawStyle(style.type, chars, first, styleTo - drawTextFrom, fromStart, toEnd, canvas, paint);
		}
	}

	protected Point scaleImage(UString.ImageValue imageValue, int maxWidth, int maxHeight)
	{
		Bitmap image = imageValue.getImage();
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		if (imageWidth > maxWidth || imageHeight > maxHeight) {
			float scaleX = (float) imageWidth / (float) maxWidth;
			float scaleY = (float) imageHeight / (float) maxHeight;
			if (scaleX > scaleY)
				return new Point(maxWidth, (int) ((float) maxHeight / scaleX));
			else
				return new Point((int) ((float) maxWidth / scaleY), maxHeight);
		} else
			return new Point(imageWidth, imageHeight);
	}

	protected abstract DrawContext createDrawContext();

	protected abstract UString prepareLineForDraw(UString line);

	protected abstract List<DrawLine> wrapLine(int line, UString text, int begin, int end, DrawContext drawContext);

	protected abstract void drawStyle(TextStyleType type, List<DrawChar> chars, int from, int to, boolean fromStart, boolean toEnd, Canvas canvas, Paint paint);
}
