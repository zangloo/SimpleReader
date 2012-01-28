package zhang.lu.SimpleReader.Book;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午2:00
 */
public class ImageContent extends BookContent
{
	ArrayList<Bitmap> images = new ArrayList<Bitmap>();

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
	public int imageCount()
	{
		return images.size();
	}

	@Override
	public Bitmap image(int index)
	{
		return images.get(index);
	}


}
