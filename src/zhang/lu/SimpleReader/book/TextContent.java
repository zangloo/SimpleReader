package zhang.lu.SimpleReader.book;

import android.graphics.Bitmap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午9:40
 */
public abstract class TextContent extends Content
{
	public Type type()
	{
		return Type.text;
	}

	public int imageCount()
	{
		return 0;
	}

	public Bitmap image(int index)
	{
		return null;
	}
}
