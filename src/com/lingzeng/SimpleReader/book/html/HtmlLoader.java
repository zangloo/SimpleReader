package com.lingzeng.SimpleReader.book.html;

import com.lingzeng.SimpleReader.Config;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.book.*;
import com.lingzeng.SimpleReader.vfs.VFile;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
		List<ContentLine> lines = new ArrayList<>();
		String cs;

		InputStream fs = f.getInputStream();
		cs = BookUtil.detect(fs);
		fs.close();

		BookUtil.HTML2Text(Jsoup.parse(f.getInputStream(), cs, "").body(), lines);
		return new SingleChapterBook(new ContentBase(lines));
	}
}
