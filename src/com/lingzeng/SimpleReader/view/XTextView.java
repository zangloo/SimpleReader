package com.lingzeng.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.ImageContent;
import com.lingzeng.SimpleReader.UString;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:31
 */
public class XTextView extends SimpleTextView
{
	private int mll;

	private static class ViewLineInfo
	{
		int line;        // index of book content
		int offset;        // offset of the line for draw
		char[] str;        // char array of line for draw

		ViewLineInfo(int l, int o, char[] s)
		{
			line = l;
			offset = o;
			str = s;
		}
	}

	private final ArrayList<ViewLineInfo> vls = new ArrayList<>();

	public XTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected int calcPosOffset(int npo)
	{
		if (reset)
			return npo;
		ContentLine line = content.line(pi);
		if (npo >= line.length())
			return 0;
		int o, to = 0;
		if (line.isImage())
			return npo;
		do {
			o = to;
			to = calcChars((UString) line, o, npo + 1);
		} while (to <= npo);
		return o;
	}

	@Override
	protected int calcNextLineOffset()
	{
		if (reset)
			return -1;
		ContentLine line = content.line(pi);
		if (line.isImage())
			return -1;
		int npo = calcChars((UString) line, po, line.length());

		return (npo == line.length()) ? -1 : npo;
	}

	@Override
	protected void resetValues()
	{
		maxLinePerPage = (int) ((pageHeight - boardGAP * 2) / fontHeight);
		xoffset = boardGAP;
		yoffset = (pageHeight - (fontHeight * maxLinePerPage)) / 2 - fontDescent;
		mll = pageWidth - 2 * boardGAP;
	}

	@Override
	protected boolean calcPrevPos()
	{
		if ((pi == 0) && (po == 0))
			return false;

		int lineCount = 0, currentPosition;
		ContentLine line;

		if (po == 0) {
			pi--;
			line = content.line(pi);
			if (line.isImage()) {
				po = 0;
				return true;
			}
			currentPosition = line.length();
		} else {
			line = content.line(pi);
			currentPosition = po;
		}

		ArrayList<Integer> li = new ArrayList<>();
		while (true) {
			lineCount += calcLines((UString) line, currentPosition, li);
			if (lineCount >= maxLinePerPage) {
				po = li.get(lineCount - maxLinePerPage);
				return true;
			}
			if (pi == 0)
				break;
			line = content.line(pi - 1);
			if (line.isImage())
				break;
			pi--;
			currentPosition = line.length();
		}
		po = 0;
		return true;
	}

	private static final char[] OC = {9};
	private static final char[] NC = {'　'};

	@Override
	protected void drawText(Canvas canvas)
	{
		int lc = 0;
		float y;

		nextpi = pi;
		nextpo = po;
		y = yoffset + fontHeight;

		UString line = null;
		vls.clear();
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
					line = ((UString) contentLine).replaceChars(OC, NC);
			}
			int p;
			p = calcChars(line, nextpo, line.length());
			if (p > nextpo) {
				char[] s = line.substring(nextpo, p).toCharArray();
				vls.add(new ViewLineInfo(nextpi, nextpo, s));
				canvas.drawText(s, 0, s.length, xoffset, y, paint);
				if ((highlightInfo != null) && (highlightInfo.line == nextpi) && (highlightInfo.end > nextpo) && (highlightInfo.begin < p)) {
					int b = Math.max(highlightInfo.begin, nextpo);
					int e = Math.min(highlightInfo.end, p);
					int x1 = (int) (calcWidth(line, nextpo, b) + xoffset);
					int y1 = (int) (y - fontHeight + fontDescent);
					int x2 = (int) (x1 + calcWidth(line, b, e));
					int y2 = (int) (y1 + fontHeight);
					Rect r = new Rect(x1, y1, x2, y2);
					canvas.drawRect(r, paint);
					paint.setColor(bcolor);
					canvas.drawText(s, b - nextpo, e - b, x1, y, paint);
					paint.setColor(color);
				}
			} else
				vls.add(null);
			y += fontHeight;
			lc++;
			if (p == line.length()) {
				nextpo = 0;
				nextpi++;
				line = null;
			} else
				nextpo = p;
		} while ((nextpi < content.lineCount()) & (lc < maxLinePerPage));
	}

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((y - yoffset - fontDescent) / fontHeight);
		if ((l < 0) || (l >= maxLinePerPage))
			return null;

		if (l >= vls.size())
			return null;
		ViewLineInfo vli = vls.get(l);
		if (vli == null)
			return null;
		float[] w = new float[vli.str.length];
		paint.getTextWidths(vli.str, 0, vli.str.length, w);
		x -= xoffset;
		for (int i = 0; i < w.length; i++)
			if ((x -= w[i]) < 0) {
				FingerPosInfo pi = new FingerPosInfo();
				pi.line = vli.line;
				pi.offset = vli.offset + i;
				return pi;
			}
		return null;
	}

	private float calcWidth(UString line, int posFrom, int posTo)
	{
		return paint.measureText(line.toString(), posFrom, posTo);
	}

	private int calcChars(UString line, int posFrom, int posTo)
	{
		return breakTextChars(line, posFrom, posTo) + posFrom;
	}

	private int calcLines(UString line, int posTo, ArrayList<Integer> li)
	{
		int i = 0, lc = 0;

		li.clear();
		do {
			li.add(i);
			i += breakTextChars(line, i, posTo);
			lc++;
		} while (i < posTo);
		return lc;
	}

	private int breakTextChars(UString line, int posFrom, int posTo)
	{
		int f, t;
		f = line.index16(posFrom);
		t = line.index16(posTo);
		int count = paint.breakText(line.toString(), f, t, true, mll, null);
		return line.count32(f, f + count);
	}
}
