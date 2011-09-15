package zhang.lu.SimpleReader.View;

import android.content.Context;
import android.graphics.Canvas;
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
	private static final int ascii_offset = 32;
	private static final int ascii_char_count = 223;
	private static final String special_chars = "“”·‘’；：…　［］｛｝□◇△○☆〖〗■◆▲●★【】『』";

	private float[] ascii_width = null;
	private float[] special_chars_width = null;

	private ArrayList<Integer> pl = new ArrayList<Integer>();
	private int pli = 0;
	private int mll;

	private float[][] charMapCache;
	private int[] fingerPosIndex;
	private int[] fingerPosOffset;
	private float minCharWidth;
	private int maxLineCharCount;

	public XTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void fontCalc()
	{
		super.fontCalc();
		if (ascii_width == null) {
			ascii_width = new float[ascii_char_count];
			special_chars_width = new float[special_chars.length()];
		}
		minCharWidth = 800;
		char[] c = new char[1];
		for (int i = 0; i < ascii_char_count; i++) {
			c[0] = (char) (i + ascii_offset);
			ascii_width[i] = paint.measureText(c, 0, 1);
			if (minCharWidth > ascii_width[i])
				minCharWidth = ascii_width[i];
		}
		for (int i = 0; i < special_chars.length(); i++) {
			special_chars_width[i] = paint.measureText(special_chars, i, i + 1);
			if (minCharWidth > special_chars_width[i])
				minCharWidth = special_chars_width[i];
		}
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
		} while (to < npo + 1);
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
		maxLineCharCount = (int) (mll / minCharWidth);
		charMapCache = new float[ml][maxLineCharCount];
		fingerPosIndex = new int[ml];
		fingerPosOffset = new int[ml];
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

		do {
			String line = replaceTextChar(content.line(nextpi).toCharArray(), OC, NC);

			int p;
			p = calcChars(line, nextpo, line.length(), lc);
			charMapCache[lc][p - nextpo] = 0;
			fingerPosIndex[lc] = nextpi;
			fingerPosOffset[lc] = nextpo;
			if (p > nextpo)
				canvas.drawText(line, nextpo, p, xoffset, y, paint);
			y += fh;
			lc++;
			if (p == line.length()) {
				nextpo = 0;
				nextpi++;
			} else
				nextpo = p;
		} while ((nextpi < content.getLineCount()) & (lc < ml));
		for (int i = lc; i < ml; i++)
			charMapCache[i][0] = 0;
	}

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((y - yoffset - fd) / fh);
		if (l < 0)
			l = 0;
		if (l >= ml)
			l = ml - 1;

		float xo = x - xoffset;
		for (int i = 0; (charMapCache[l][i] != 0) && (i < maxLineCharCount); i++)
			if (xo <= charMapCache[l][i]) {
				i += fingerPosOffset[l];
				FingerPosInfo pi = new FingerPosInfo();
				pi.line = fingerPosIndex[l];
				pi.offset = i;
				return pi;
			}
		return null;
	}

	private int calcChars(String line, int posFrom, int posTo)
	{
		return calcChars(line, posFrom, posTo, -1);
	}

	private int calcChars(String line, int posFrom, int posTo, int lineNum)
	{
		int i;
		float ll = 0;
		int j = 0;

		for (i = posFrom; i < posTo; i++) {
			char ch = line.charAt(i);
			ll += charWidth(ch);
			if (ll > mll)
				return i;
			if (lineNum >= 0)
				charMapCache[lineNum][j++] = ll;
		}
		return i;
	}

	private int calcLines(String line, int posTo)
	{
		int i, lc = 0;
		float ll = 0;
		pl.clear();
		pl.add(0);
		for (i = 0; i < posTo; i++) {
			float d = charWidth(line.charAt(i));
			if (ll + d > mll) {
				lc++;
				ll = d;
				pl.add(i);
			} else
				ll += d;
		}
		return lc + 1;
	}

	private float charWidth(char c)
	{
		int i;

		if ((c >= ascii_offset) && (c <= ascii_offset + ascii_char_count))
			return ascii_width[c - ascii_offset];
		else if ((i = special_chars.indexOf(c)) >= 0)
			return special_chars_width[i];
		else
			return fw;
	}
}
