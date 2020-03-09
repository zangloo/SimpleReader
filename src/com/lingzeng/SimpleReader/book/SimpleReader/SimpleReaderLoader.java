package com.lingzeng.SimpleReader.book.SimpleReader;

import com.lingzeng.SimpleReader.Config;
import com.lingzeng.SimpleReader.vfs.CloudFile;
import com.lingzeng.SimpleReader.vfs.VFile;
import com.lingzeng.SimpleReader.book.BookLoader;

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

	public com.lingzeng.SimpleReader.book.Book load(VFile f, Config.ReadingInfo ri) throws Exception
	{
		return new SimpleReaderBook(f, ri);
	}
}
