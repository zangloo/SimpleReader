package zhang.lu.SimpleReader.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-6
 * Time: 下午12:10
 */
public class HTextView extends SimpleTextView
{
	public static final char[] SC = {'「', '」', '『', '』', '（', '）', '《', '》', '〔', '〕', '【', '】', '｛', '｝', '─','…', 9, '(', ')', '[', ']', '<', '>', '{', '}'};
	public static final char[] TC = {'﹁', '﹂', '﹃', '﹄', '︵', '︶', '︽', '︾', '︹', '︺', '︻', '︼', '︷', '︸', '︱','⋮', '　', '︵', '︶', '︹', '︺', '︻', '︼', '︷', '︸'};

	private int mc;
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
		fw *= 1.15;
	}

	@Override
	protected void drawText(Canvas canvas)
	{
		int lc = 0;
		float x, y;
		float[] xy = new float[mc * 2];
		char[] buf = new char[mc];

		nextpi = pi;
		nextpo = po;
		x = w - xoffset - fw;

		String line = null;
		do {
			if (line == null)
				line = replaceTextChar(content.line(nextpi).toCharArray(), SC, TC);
			y = yoffset + fh;
			int cc = line.length() - nextpo;
			if (cc > mc)
				cc = mc;
			fingerPosIndex[lc] = nextpi;
			fingerPosOffset[lc] = nextpo;
			int i = 0;
			for (; i < cc; i++) {
				buf[i] = line.charAt(nextpo + i);
				xy[i * 2] = x;
				xy[i * 2 + 1] = y;
				y += fh;
			}
			if (cc > 0) {
				canvas.drawPosText(buf, 0, cc, xy, paint);
				if ((hli != null) && (hli.line == nextpi) && (hli.end > nextpo) &&
					(hli.begin < nextpo + i)) {
					int b = Math.max(hli.begin, nextpo);
					int e = Math.min(hli.end, nextpo + i);
					for (int j = 0; j < e - b; j++) {
						xy[j * 2] = xy[(b - nextpo + j) * 2];
						xy[j * 2 + 1] = xy[(b - nextpo + j) * 2 + 1];
					}
					Rect r = new Rect((int) xy[0], (int) (xy[1] - fh + fd),
							  (int) (xy[(e - b - 1) * 2] + fw),
							  (int) (xy[(e - b - 1) * 2 + 1] + fd));
					canvas.drawRect(r, paint);
					paint.setColor(bcolor);
					canvas.drawPosText(buf, b - nextpo, e - b, xy, paint);
					paint.setColor(color);
				}
			}
			lc++;
			x -= fw;

			nextpo += i;
			if (nextpo == line.length()) {
				nextpo = 0;
				nextpi++;
				line = null;
			}
		} while ((nextpi < content.getLineCount()) & (lc < ml));
		for (int i = lc; i < ml; i++)
			fingerPosIndex[i] = -1;
	}

	@Override
	protected FingerPosInfo calcFingerPos(float x, float y)
	{
		int l = (int) ((w - xoffset - x) / fw);
		int c = (int) ((y - yoffset - fd) / fh);
		if ((l < 0) ||(c < 0) ||  (l >= ml) || (c >= mc))
			return null;
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

		int lc = (po + mc - 1) / mc;

		while ((lc < ml) && (pi > 0)) {
			po = content.line(--pi).length();
			if (po == 0)
				lc++;
			else
				lc += (po + mc - 1) / mc;
		}
		if (lc > ml)
			po = (lc - ml) * mc;
		if (lc <= ml)
			po = 0;
		return true;
	}

	@Override
	protected void resetValues()
	{
		mc = (int) ((h - boardGAP * 2) / fh);
		ml = (int) ((w - boardGAP * 2) / fw);
		xoffset = (w - (fw * ml)) / 2;
		yoffset = (h - (fh * mc)) / 2 - fd;
		fingerPosIndex = new int[ml];
		fingerPosOffset = new int[ml];
	}

	@Override
	protected int calcPosOffset(int npo)
	{
		if (mc == 0)
			return npo;
		return (npo / mc) * mc;
	}

	@Override
	public int calcNextLineOffset()
	{
		int npo = po + mc;
		return (npo < content.line(pi).length()) ? npo : -1;
	}

}

