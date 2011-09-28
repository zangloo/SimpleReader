package zhang.lu.SimpleReader.Book;

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

	public String[] getSuffixes()
	{
		return suffixes;
	}

	public BookContent load(VFile f) throws Exception
	{
		List<String> lines = new ArrayList<String>();
		String cs;

		InputStream fs = f.getInputStream();
		cs = BookUtil.detect(fs);
		fs.close();

		BookUtil.HTML2Text(Jsoup.parse(f.getInputStream(), cs, "").body(), lines);
		return new PlainTextContent(lines);
	}

	public void unload(BookContent aBook)
	{
	}
}
