package com.lingzeng.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import com.lingzeng.SimpleReader.ImageContent;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.UString;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-6
 * Time: 下午12:10
 */
public class HTextView extends SimpleTextView
{
	public static final char[] SC = {'「', '」', '『', '』', '（', '）', '《', '》', '〔', '〕', '【', '】', '｛', '｝', '─', '…', 9, '(', ')', '[', ']', '<', '>', '{', '}', '-', '—'};
	public static final char[] TC = {'﹁', '﹂', '﹃', '﹄', '︵', '︶', '︽', '︾', '︹', '︺', '︻', '︼', '︷', '︸', '︱', '⋮', '　', '︵', '︶', '︹', '︺', '︻', '︼', '︷', '︸', '︱', '︱'};

	private int maxCharPerLine;
	private int[] fingerPosIndex;
	private int[] fingerPosOffset;

	public HTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void fontCalc()
	{
		super.fontCalc();
		fontWidth *= 1.15;
	}

	@Override
	protected void drawText(Canvas canvas)
	{
		int lineCount = 0;
		float x, y;
		float[] drawingPositions = new float[maxCharPerLine * 2 * 2];
		char[] buf = new char[maxCharPerLine * 2];

		nextpi = pi;
		nextpo = po;
		x = pageWidth - xoffset - fontWidth;

		UString line = null;
		do {
			if (line == null) {
				ContentLine contentLine = content.line(nextpi);
				if (contentLine.isImage()) {
					if (nextpi == pi) {
						drawImage(canvas, (ImageContent) contentLine);
						nextpi++;
						return;
					} else
						break;
				} else
					line = ((UString) contentLine).replaceChars(SC, TC);
			}
			int charCount = line.length() - nextpo;
			if (charCount > maxCharPerLine)
				charCount = maxCharPerLine;
			float yStart = yoffset;
			if (nextpo == 0 && line.isParagraph()) {
				int space = maxCharPerLine - charCount;
				if (space < 2)
					charCount -= 2 - space;
				yStart += fontHeight * 2;
			}
			y = yStart + fontHeight;

			fingerPosIndex[lineCount] = nextpi;
			fingerPosOffset[lineCount] = nextpo;
			int len = 0;
			int count16 = 0;
			for (; len < charCount; len++) {
				int ch = line.charAt(nextpo + len);
				count16 += Character.toChars(ch, buf, count16);
				drawingPositions[len * 2] = x;
				drawingPositions[len * 2 + 1] = y;
				y += fontHeight;
			}
			if (charCount > 0) {
				canvas.drawPosText(buf, 0, count16, drawingPositions, paint);
				if ((highlightInfo != null) && (highlightInfo.line == nextpi) && (highlightInfo.end > nextpo) &&
					(highlightInfo.begin < nextpo + len)) {
					int b = Math.max(highlightInfo.begin, nextpo);
					int e = Math.min(highlightInfo.end, nextpo + len);
					for (int j = 0; j < e - b; j++) {
						drawingPositions[j * 2] = drawingPositions[(b - nextpo + j) * 2];
						drawingPositions[j * 2 + 1] = drawingPositions[(b - nextpo + j) * 2 + 1];
					}
					Rect r = new Rect((int) drawingPositions[0], (int) (drawingPositions[1] - fontHeight + fontDescent),
						(int) (drawingPositions[(e - b - 1) * 2] + fontWidth),
						(int) (drawingPositions[(e - b - 1) * 2 + 1] + fontDescent));
					canvas.drawRect(r, paint);
					paint.setColor(bcolor);
					canvas.drawPosText(buf, b - nextpo, line.count16(b, e), drawingPositions, paint);
					paint.setColor(color);
				}
			}
			lineCount++;
			drawUnderline(line, nextpo, nextpo + len, x, yStart, fontHeight, fontDescent, canvas, paint);
			x -= fontWidth;

			nextpo += len;
			if (nextpo == line.length()) {
				nextpo = 0;
				nextpi++;
				line = null;
			}
		} while ((nextpi < content.lineCount()) & (lineCount < maxLinePerPage));
		for (int i = lineCount; i < maxLinePerPage; i++)
			fingerPosIndex[i] = -1;
	}

	private void drawUnderline(UString line, int charFrom, int charTo, float x, float y, float fontHeight, float fontDescent, Canvas canvas, Paint paint)
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

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((pageWidth - xoffset - x) / fontWidth);
		int c = (int) ((y - yoffset - fontDescent) / fontHeight);
		if (l < 0)
			l = 0;
		else if (l >= maxLinePerPage)
			l = maxLinePerPage - 1;
		if (c < 0)
			c = 0;
		else if (c >= maxCharPerLine)
			c = maxCharPerLine - 1;
		if (fingerPosIndex[l] == -1)
			return null;
		int i = fingerPosOffset[l] + c;

		if (i >= content.line(fingerPosIndex[l]).length())
			return null;
		FingerPosInfo pi = new FingerPosInfo();
		pi.line = fingerPosIndex[l];
		pi.offset = i;
		return pi;
	}

	@Override
	protected boolean calcPrevPos()
	{
		if ((pi == 0) && (po == 0))
			return false;

		int lc = (po + maxCharPerLine - 1) / maxCharPerLine;
		boolean lineChanged = false;
		while ((lc < maxLinePerPage) && (pi > 0)) {
			ContentLine line = content.line(pi - 1);
			if (line.isImage()) {
				po = 0;
				if (!lineChanged)
					pi--;
				return true;
			}
			lineChanged = true;
			pi--;
			po = line.length();
			if (po == 0)
				lc++;
			else
				lc += (po + maxCharPerLine - 1) / maxCharPerLine;
		}
		if (lc > maxLinePerPage)
			po = (lc - maxLinePerPage) * maxCharPerLine;
		if (lc <= maxLinePerPage)
			po = 0;
		return true;
	}

	@Override
	protected void resetValues()
	{
		maxCharPerLine = (int) ((pageHeight - boardGAP * 2) / fontHeight);
		maxLinePerPage = (int) ((pageWidth - boardGAP * 2) / fontWidth);
		xoffset = (pageWidth - (fontWidth * maxLinePerPage)) / 2;
		yoffset = (pageHeight - (fontHeight * maxCharPerLine)) / 2 - fontDescent;
		fingerPosIndex = new int[maxLinePerPage];
		fingerPosOffset = new int[maxLinePerPage];
	}

	@Override
	protected int calcPosOffset(int npo)
	{
		if (maxCharPerLine == 0)
			return npo;
		return (npo / maxCharPerLine) * maxCharPerLine;
	}

	@Override
	protected int calcNextLineOffset()
	{
		int npo = po + maxCharPerLine;
		return (npo < content.line(pi).length()) ? npo : -1;
	}

}

