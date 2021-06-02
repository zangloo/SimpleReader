package com.lingzeng.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.lingzeng.SimpleReader.ContentImage;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.UString;

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
		int lc = 0;
		float x, y;
		float[] xy = new float[maxCharPerLine * 2 * 2];
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
						drawImage(canvas, (ContentImage) contentLine);
						nextpi++;
						return;
					} else
						break;
				} else
					line = ((UString) contentLine).replaceChars(SC, TC);
			}
			y = yoffset + fontHeight;
			int cc = line.length() - nextpo;
			if (cc > maxCharPerLine)
				cc = maxCharPerLine;
			fingerPosIndex[lc] = nextpi;
			fingerPosOffset[lc] = nextpo;
			int len = 0;
			int count16 = 0;
			for (; len < cc; len++) {
				int ch = line.charAt(nextpo + len);
				count16 += Character.toChars(ch, buf, count16);
				xy[len * 2] = x;
				xy[len * 2 + 1] = y;
				y += fontHeight;
			}
			if (cc > 0) {
				canvas.drawPosText(buf, 0, count16, xy, paint);
				if ((hli != null) && (hli.line == nextpi) && (hli.end > nextpo) &&
					(hli.begin < nextpo + len)) {
					int b = Math.max(hli.begin, nextpo);
					int e = Math.min(hli.end, nextpo + len);
					for (int j = 0; j < e - b; j++) {
						xy[j * 2] = xy[(b - nextpo + j) * 2];
						xy[j * 2 + 1] = xy[(b - nextpo + j) * 2 + 1];
					}
					Rect r = new Rect((int) xy[0], (int) (xy[1] - fontHeight + fontDescent),
						(int) (xy[(e - b - 1) * 2] + fontWidth),
						(int) (xy[(e - b - 1) * 2 + 1] + fontDescent));
					canvas.drawRect(r, paint);
					paint.setColor(bcolor);
					canvas.drawPosText(buf, b - nextpo, line.count16(b, e), xy, paint);
					paint.setColor(color);
				}
			}
			lc++;
			x -= fontWidth;

			nextpo += len;
			if (nextpo == line.length()) {
				nextpo = 0;
				nextpi++;
				line = null;
			}
		} while ((nextpi < content.lineCount()) & (lc < maxLinePerPage));
		for (int i = lc; i < maxLinePerPage; i++)
			fingerPosIndex[i] = -1;
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

