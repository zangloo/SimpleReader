package zhang.lu.SimpleReader.Book;

import android.graphics.Bitmap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午2:00
 */
public abstract class ImageContent extends BookContent
{
	@Override
	public String line(int index)
	{
		return null;
	}

	@Override
	public int getLineCount()
	{
		return 0;
	}

	@Override
	public int size(int end)
	{
		return 0;
	}

	@Override
	public int size()
	{
		return 0;
	}

	@Override
	public Type type()
	{
		return Type.image;
	}

	@Override
	public abstract int imageCount();

	@Override
	public abstract Bitmap image(int index);
}
