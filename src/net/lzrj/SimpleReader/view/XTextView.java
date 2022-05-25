package net.lzrj.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import net.lzrj.SimpleReader.UString;

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
			float charWidth = measureChar(text.charAt(i));
			if (left + charWidth > max) {
				drawContext.baseline += drawLine.drawSize + drawLine.space;
				drawLine = createDrawLine(line, fontMeasure);
				lines.add(drawLine);
				left = viewMargin;
			}
			if (i == begin || fontMeasure.height > drawLine.drawSize) {
				drawLine.drawSize = fontMeasure.height;
				drawLine.space = fontMeasure.height / 2;
			}
			Rect rect = new Rect((int) left, drawContext.baseline, (int) (left + charWidth), drawContext.baseline + (int) fontMeasure.height);
			drawLine.chars.add(new DrawChar(i, fontSize, rect));
			left += charWidth;
		}
		drawContext.baseline += drawLine.drawSize + drawLine.space;
		return lines;
	}

	@Override
	protected void drawUnderline(UString line, int charFrom, int charTo, float x, float y, float fontHeight, float fontDescent, Canvas canvas, Paint paint)
	{
	}
}
