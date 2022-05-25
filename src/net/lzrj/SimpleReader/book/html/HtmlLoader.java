package net.lzrj.SimpleReader.book.html;

import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.book.*;
import net.lzrj.SimpleReader.vfs.VFile;
import org.jsoup.Jsoup;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:23
 */

public class HtmlLoader implements BookLoader.Loader
{
	private static final String[] suffixes = {"htm", "html"};

	public boolean isBelong(VFile f)
	{
		for (String s : suffixes)
			if (f.getPath().toLowerCase().endsWith("." + s))
				return true;
		return false;
	}

	public Book load(VFile f, Config.ReadingInfo ri) throws Exception
	{
		String cs;

		InputStream fs = f.getInputStream();
		cs = BookUtil.detectCharset(fs);
		fs.close();

		BookUtil.HtmlContent htmlContent = BookUtil.HTML2Text(Jsoup.parse(f.getInputStream(), cs, ""));
		return new SingleChapterBook(new ContentBase(htmlContent.lines));
	}
}
