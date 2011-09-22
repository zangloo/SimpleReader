package zhang.lu.SimpleReader.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import zhang.lu.SimpleReader.Book.BookContent;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-10
 * Time: 下午5:31
 */
public class XTextView extends SimpleTextView
{
	private ArrayList<Integer> pl = new ArrayList<Integer>();
	private int pli = 0;
	private int mll;

	private class ViewLineInfo
	{
		int line;	// index of book content
		int offset;	// offset of the line for draw
		char[] str;	// char array of line for draw

		ViewLineInfo(int l, int o, char[] s)
		{
			line = l;
			offset = o;
			str = s;
		}
	}

	private ArrayList<ViewLineInfo> vls = new ArrayList<ViewLineInfo>();

	public XTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public void setPos(int posIndex, int posOffset)
	{
		super.setPos(posIndex, posOffset);
		clearpl();
	}

	@Override
	protected int calcPosOffset(int npo)
	{
		if (reset)
			return npo;
		int o, to = 0;
		String line = content.line(pi);
		do {
			o = to;
			to = calcChars(line, o, npo + 1);
		} while (to <= npo);
		return o;
	}

	@Override
	public int calcNextLineOffset()
	{
		if (reset)
			return -1;
		String line = content.line(pi);
		int npo = calcChars(line, po, line.length());

		return (npo == line.length()) ? -1 : npo;
	}

	private void clearpl()
	{
		pli = 0;
		pl.clear();
	}

	@Override
	public void setColorAndFont(int color, int aBcolor, int fontSize, Typeface typeface)
	{
		super.setColorAndFont(color, aBcolor, fontSize, typeface);
		clearpl();
	}

	@Override
	public void setContent(BookContent newContent)
	{
		super.setContent(newContent);
		clearpl();
	}

	@Override
	protected void resetValues()
	{
		ml = (int) ((h - boardGAP * 2) / fh);
		xoffset = boardGAP;
		yoffset = (h - (fh * ml)) / 2 - fd;
		mll = w - 2 * boardGAP;
		clearpl();
	}

	@Override
	protected boolean calcNextPos()
	{
		if (super.calcNextPos()) {
			clearpl();
			return true;
		}
		return false;
	}

	@Override
	protected boolean calcPrevPos()
	{
		if ((pi == 0) && (po == 0))
			return false;

		int lc = 0, cp;
		String line;

		if (pli >= ml) {
			pli -= ml;
			po = pl.get(pli);
			if (pli == 0)
				pl.clear();
			return true;
		}
		if (pli > 0) {
			lc = pli;
			po = 0;
			clearpl();
			if (pi == 0)
				return true;
		}
		if (po == 0) {
			pi--;
			line = content.line(pi);
			cp = line.length();
		} else {
			line = content.line(pi);
			cp = po;
		}

		while (true) {
			lc += calcLines(line, cp);
			if (lc >= ml) {
				pli = lc - ml;
				po = pl.get(lc - ml);
				if (pli == 0)
					pl.clear();
				return true;
			}
			if (pi == 0)
				break;
			line = content.line(--pi);
			cp = line.length();
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
		y = yoffset + fh;

		String line = null;
		vls.clear();
		do {
			if (line == null)
				line = replaceTextChar(content.line(nextpi).toCharArray(), OC, NC);
			int p;
			p = calcChars(line, nextpo, line.length());
			if (p > nextpo) {
				char[] s = line.substring(nextpo, p).toCharArray();
				vls.add(new ViewLineInfo(nextpi, nextpo, s));
				canvas.drawText(s, 0, s.length, xoffset, y, paint);
				if ((hli != null) && (hli.line == nextpi) && (hli.end > nextpo) && (hli.begin < p)) {
					int b = Math.max(hli.begin, nextpo);
					int e = Math.min(hli.end, p);
					int x1 = (int) (calcWidth(line, nextpo, b) + xoffset);
					int y1 = (int) (y - fh + fd);
					int x2 = (int) (x1 + calcWidth(line, b, e));
					int y2 = (int) (y1 + fh);
					Rect r = new Rect(x1, y1, x2, y2);
					canvas.drawRect(r, paint);
					paint.setColor(bcolor);
					canvas.drawText(s, b - nextpo, e - b, x1, y, paint);
					paint.setColor(color);
				}
			} else
				vls.add(null);
			y += fh;
			lc++;
			if (p == line.length()) {
				nextpo = 0;
				nextpi++;
				line = null;
			} else
				nextpo = p;
		} while ((nextpi < content.getLineCount()) & (lc < ml));
	}

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((y - yoffset - fd) / fh);
		if (l < 0)
			l = 0;
		if (l >= ml)
			l = ml - 1;

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

	private float calcWidth(String line, int posFrom, int posTo)
	{
		return paint.measureText(line, posFrom, posTo);
	}

	private int calcChars(String line, int posFrom, int posTo)
	{
		return paint.breakText(line, posFrom, posTo, true, mll, null) + posFrom;
	}

	private int calcLines(String line, int posTo)
	{
		int i = 0, c, lc = 0;
		pl.clear();

		do {
			pl.add(i);
			c = paint.breakText(line, i, posTo, true, mll, null);
			i += c;
			lc++;
		} while (i < posTo);
		return lc;
	}
}
