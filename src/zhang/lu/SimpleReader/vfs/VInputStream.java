package zhang.lu.SimpleReader.vfs;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午11:38
 */
public class VInputStream extends InputStream
{
	InputStream zis;

	VInputStream(InputStream is)
	{
		zis = is;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		return zis.read(b, off, len);
	}

	@Override
	public int read() throws IOException
	{
		return zis.read();
	}

	@Override
	public int read(byte b[]) throws IOException
	{
		int c = 0;
		int d;
		do {
			if ((d = zis.read(b, c, b.length - c)) < 0)
				return (c == 0) ? -1 : c;
			c += d;
		} while (c < b.length);

		return c;
	}
}
