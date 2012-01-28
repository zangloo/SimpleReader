package zhang.lu.SimpleReader.book;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午2:00
 */
public abstract class ImageContent extends Content
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
}
