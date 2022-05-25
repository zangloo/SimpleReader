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
 * Date: 10-12-10
 * Time: 下午5:31
 */
public class XTextView extends SimpleTextView
{
	private static final char[] OC = {9};
	private static final char[] NC = {'　'};

	public XTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected DrawContext createDrawContext()
	{
		int maxDrawSize = pageHeight - 2 * viewMargin;
		int maxLineSize = pageWidth - 2 * viewMargin;
		int baseline = viewMargin;
		return new DrawContext(maxDrawSize, maxLineSize, baseline);
	}

	@Override
	protected UString prepareLineForDraw(UString line)
	{
		return line.replaceChars(OC, NC);
	}

	private DrawLine createDrawLine(int line, FontMeasure fontMeasure)
	{
		float height = fontMeasure.height;
		float space = height / 4;
		return new DrawLine(line, height, space);
	}

	@Override
	protected List<DrawLine> wrapLine(int line, UString text, int begin, int end, DrawContext drawContext)
	{
		List<DrawLine> lines = new ArrayList<>();
		FontMeasure defaultFontMeasure = fontMeasure(fontSize);
		DrawLine drawLine = createDrawLine(line, defaultFontMeasure);
		float left = viewMargin;
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
				left += 2 * defaultFontMeasure.width;
			float drawWidth = measureChar(text.charAt(i));

			// calc char draw width, include border margin
			List<TextContentBase.TextStyle> styles = text.styles();
			Point drawOffset = null;
			if (styles != null)
				for (TextContentBase.TextStyle style : styles)
					if (TextStyleType.border.equals(style.type))
						if (style.from == i) {
							int margin = (int) drawWidth >> 2;
							drawOffset = new Point(margin, 0);
							drawWidth += margin;
							break;
						} else if (style.to - 1 == i) {
							int margin = (int) drawWidth >> 2;
							drawWidth += margin;
							break;
						}

			if (left + drawWidth > max) {
				drawContext.baseline += drawLine.drawSize + drawLine.space;
				drawLine = createDrawLine(line, fontMeasure);
				lines.add(drawLine);
				left = viewMargin;
			}
			if (i == begin || fontMeasure.height > drawLine.drawSize) {
				drawLine.drawSize = fontMeasure.height;
				drawLine.space = fontMeasure.height / 4;
			}
			float right = left + drawWidth;
			Rect rect = new Rect((int) left, drawContext.baseline, (int) right, drawContext.baseline + (int) fontMeasure.height);
			drawLine.chars.add(new DrawChar(i, fontSize, rect, drawOffset));
			left = right;
		}
		drawContext.baseline += drawLine.drawSize + drawLine.space;
		return lines;
	}

	@Override
	protected void drawStyle(TextStyleType type, List<DrawChar> chars, int from, int to, boolean fromStart, boolean toEnd, Canvas canvas, Paint paint)
	{
		int maxHeight = 0;
		for (int i = from; i < to; i++) {
			DrawChar dc = chars.get(i);
			Rect rect = dc.rect;
			int height = rect.bottom - rect.top;
			if (maxHeight < height)
				maxHeight = height;
		}
		// should never happen
		if (maxHeight == 0)
			return;
		DrawChar dc = chars.get(from);
		Rect rect = dc.rect;
		int left = rect.left;
		int bottom = rect.bottom;
		dc = chars.get(to - 1);
		int right = dc.rect.right;
		int margin = maxHeight >> 3;
		switch (type) {
			case underline:
				if (fromStart)
					left += margin;
				if (toEnd)
					right -= margin;
				left -= margin;
				canvas.drawLine(left, bottom, right, bottom, paint);
				break;
			case border:
				int top = bottom - maxHeight - margin;
				bottom += margin;
				left += margin;
				right -= margin;
				canvas.drawLine(left, top, right, top, paint);
				canvas.drawLine(left, bottom, right, bottom, paint);
				if (fromStart)
					canvas.drawLine(left, top, left, bottom, paint);
				if (toEnd)
					canvas.drawLine(right, top, right, bottom, paint);
				break;
		}
	}
}
