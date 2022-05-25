package net.lzrj.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import net.lzrj.SimpleReader.TextContentBase;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.TextStyleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-6
 * Time: 下午12:10
 */
public class HTextView extends SimpleTextView
{
	public static final char[] SC = new char[]{'「', '」', '〈', '〉', '『', '』', '（', '）', '《', '》', '〔', '〕', '【', '】', '｛', '｝', '─', '…', 9, '(', ')', '[', ']', '<', '>', '{', '}', '-', '—', '〖', '〗'};
	public static final char[] TC = new char[]{'﹁', '﹂', '︿', '﹀', '﹃', '﹄', '︵', '︶', '︽', '︾', '︹', '︺', '︻', '︼', '︷', '︸', '︱', '⋮', '　', '︵', '︶', '︹', '︺', '︻', '︼', '︷', '︸', '︱', '︱', '\uE794', '\uE795'};

	public HTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected DrawContext createDrawContext()
	{
		int maxDrawSize = pageWidth - 2 * viewMargin;
		int maxLineSize = pageHeight - 2 * viewMargin;
		int baseline = pageWidth - viewMargin;
		return new DrawContext(maxDrawSize, maxLineSize, baseline);
	}

	@Override
	protected UString prepareLineForDraw(UString line)
	{
		return line.replaceChars(SC, TC);
	}

	private DrawLine createDrawLine(int line, FontMeasure fontMeasure)
	{
		float width = fontMeasure.width;
		float space = width / 2;
		return new DrawLine(line, width, space);
	}

	@Override
	protected List<DrawLine> wrapLine(int line, UString text, int begin, int end, DrawContext drawContext)
	{
		List<DrawLine> lines = new ArrayList<>();
		FontMeasure defaultFontMeasure = fontMeasure(fontSize);
		DrawLine drawLine = createDrawLine(line, defaultFontMeasure);
		float top = viewMargin;
		lines.add(drawLine);
		int max = drawContext.maxLineSize + viewMargin;

		int usingFontSize = 0;
		for (int i = begin; i < end; i++) {
			int fontSize = scaleFontSize(text.charSizeAt(i));
			if (usingFontSize != fontSize) {
				usingFontSize = fontSize;
				setTextSize(paint, usingFontSize);
			}
			FontMeasure fontMeasure = fontMeasure(fontSize);
			if (i == 0 && text.isParagraph())
				top += 2 * defaultFontMeasure.height;
			// calc char draw height, include border margin
			float drawHeight = fontMeasure.height;
			List<TextContentBase.TextStyle> styles = text.styles();
			Point drawOffset = null;
			if (styles != null)
				for (TextContentBase.TextStyle style : styles)
					if (TextStyleType.border.equals(style.type))
						if (style.from == i) {
							drawHeight += (int) fontMeasure.height >> 2;
							break;
						} else if (style.to - 1 == i) {
							int margin = (int) fontMeasure.height >> 2;
							drawOffset = new Point(0, margin);
							drawHeight += margin;
							break;
						}

			if (top + drawHeight > max) {
				drawContext.baseline -= drawLine.drawSize + drawLine.space;
				drawLine = createDrawLine(line, fontMeasure);
				lines.add(drawLine);
				top = viewMargin;
			}
			if (i == begin || fontMeasure.width > drawLine.drawSize) {
				drawLine.drawSize = fontMeasure.width;
				drawLine.space = fontMeasure.width / 2;
			}
			float charWidth = measureChar(text.charAt(i));
			float bottom = top + drawHeight;
			Rect rect = new Rect(drawContext.baseline - (int) charWidth, (int) top, drawContext.baseline, (int) bottom);
			drawLine.chars.add(new DrawChar(i, fontSize, rect, drawOffset));
			top = bottom;
		}
		drawContext.baseline -= drawLine.drawSize + drawLine.space;
		return lines;
	}

	@Override
	protected void drawStyle(TextStyleType type, List<DrawChar> chars, int from, int to, boolean fromStart, boolean toEnd, Canvas canvas, Paint paint)
	{
		int maxWidth = 0;
		for (int i = from; i < to; i++) {
			DrawChar dc = chars.get(i);
			Rect rect = dc.rect;
			int width = rect.right - rect.left;
			if (maxWidth < width)
				maxWidth = width;
		}
		// should never happen
		if (maxWidth == 0)
			return;
		DrawChar dc = chars.get(from);
		Rect rect = dc.rect;
		int right = rect.right;
		int left = right - maxWidth;
		int top = rect.top;
		dc = chars.get(to - 1);
		int bottom = dc.rect.bottom;
		int margin = maxWidth >> 3;
		switch (type) {
			case underline:
				if (fromStart)
					top += margin;
				if (toEnd)
					bottom -= margin;
				left -= margin;
				canvas.drawLine(left, top, left, bottom, paint);
				break;
			case border:
				top += margin;
				bottom -= margin;
				left -= margin;
				right += margin;
				canvas.drawLine(left, top, left, bottom, paint);
				canvas.drawLine(right, top, right, bottom, paint);
				if (fromStart)
					canvas.drawLine(left, top, right, top, paint);
				if (toEnd)
					canvas.drawLine(left, bottom, right, bottom, paint);
				break;
		}
	}
}

