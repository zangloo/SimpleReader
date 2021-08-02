package net.lzrj.SimpleReader.book.SimpleReader;

import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.vfs.CloudFile;
import net.lzrj.SimpleReader.vfs.VFile;
import net.lzrj.SimpleReader.book.BookLoader;

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

	public net.lzrj.SimpleReader.book.Book load(VFile f, Config.ReadingInfo ri) throws Exception
	{
		return new SimpleReaderBook(f, ri);
	}
}
