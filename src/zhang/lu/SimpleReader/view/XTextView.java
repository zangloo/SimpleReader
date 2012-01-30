package zhang.lu.SimpleReader.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

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
	protected int calcNextLineOffset()
	{
		if (reset)
			return -1;
		String line = content.line(pi);
		int npo = calcChars(line, po, line.length());

		return (npo == line.length()) ? -1 : npo;
	}

	@Override
	protected void resetValues()
	{
		ml = (int) ((h - boardGAP * 2) / fh);
		xoffset = boardGAP;
		yoffset = (h - (fh * ml)) / 2 - fd;
		mll = w - 2 * boardGAP;
	}

	@Override
	protected boolean calcPrevPos()
	{
		if ((pi == 0) && (po == 0))
			return false;

		int lc = 0, cp;
		String line;

		if (po == 0) {
			pi--;
			line = content.line(pi);
			cp = line.length();
		} else {
			line = content.line(pi);
			cp = po;
		}

		ArrayList<Integer> li = new ArrayList<Integer>();
		while (true) {
			lc += calcLines(line, cp, li);
			if (lc >= ml) {
				po = li.get(lc - ml);
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
		} while ((nextpi < content.lineCount()) & (lc < ml));
	}

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((y - yoffset - fd) / fh);
		if ((l < 0) || (l >= ml))
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

	private float calcWidth(String line, int posFrom, int posTo)
	{
		return paint.measureText(line, posFrom, posTo);
	}

	private int calcChars(String line, int posFrom, int posTo)
	{
		return paint.breakText(line, posFrom, posTo, true, mll, null) + posFrom;
	}

	private int calcLines(String line, int posTo, ArrayList<Integer> li)
	{
		int i = 0, lc = 0;

		li.clear();
		do {
			li.add(i);
			i += paint.breakText(line, i, posTo, true, mll, null);
			lc++;
		} while (i < posTo);
		return lc;
	}
}
