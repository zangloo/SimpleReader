package net.lzrj.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import net.lzrj.SimpleReader.UString;

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
			if (top + fontMeasure.height > max) {
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
			Rect rect = new Rect(drawContext.baseline - (int) charWidth, (int) top, drawContext.baseline, (int) (top + fontMeasure.height));
			drawLine.chars.add(new DrawChar(i, fontSize, rect));
			top += fontMeasure.height;
		}
		drawContext.baseline -= drawLine.drawSize + drawLine.space;
		return lines;
	}

	protected void drawUnderline(UString line, int charFrom, int charTo, float x, float y, float fontHeight, float fontDescent, Canvas canvas, Paint paint)
	{
		List<Pair<Integer, Integer>> underlines = line.underlines();
		if (underlines == null)
			return;
		x -= 4;
		for (Pair<Integer, Integer> pair : underlines) {
			boolean draw = false;
			int drawFrom = 0, drawTo = 0;
			if (pair.first >= charFrom && pair.first < charTo) {
				drawFrom = pair.first;
				if (pair.second > charTo)
					drawTo = charTo;
				else
					drawTo = pair.second;
				draw = true;
			} else if (pair.second > charFrom && pair.second <= charTo) {
				drawTo = pair.second;
				if (pair.first < charFrom)
					drawFrom = charFrom;
				else
					drawFrom = pair.first;
				draw = true;
			} else if (pair.first < charFrom && pair.second >= charTo) {
				draw = true;
				drawFrom = charFrom;
				drawTo = charTo;
			}
			if (!draw) continue;
			float yStart = y + fontDescent + fontHeight * (drawFrom - charFrom);
			float yEnd = yStart - fontDescent + fontHeight * (drawTo - drawFrom);
			canvas.drawLine(x, yStart, x, yEnd, paint);
		}
	}
}

