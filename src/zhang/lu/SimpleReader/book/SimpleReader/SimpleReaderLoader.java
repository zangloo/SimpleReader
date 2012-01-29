package zhang.lu.SimpleReader.book.SimpleReader;

import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.vfs.CloudFile;
import zhang.lu.SimpleReader.vfs.VFile;
import zhang.lu.SimpleReader.book.BookLoader;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-22
 * Time: 下午7:34
 */
public class SimpleReaderLoader implements BookLoader.Loader
{
	public static final String suffix = "srb";

	public boolean isBelong(VFile f)
	{
		return (!CloudFile.class.isInstance(f)) && (f.getPath().toLowerCase().endsWith("." + suffix));
	}

	public zhang.lu.SimpleReader.book.Book load(VFile f, Config.ReadingInfo ri) throws Exception
	{
		return new SimpleReaderBook(f, ri);
	}
}
