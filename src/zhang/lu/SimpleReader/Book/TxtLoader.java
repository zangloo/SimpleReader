package zhang.lu.SimpleReader.Book;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:23
 */

public class TxtLoader implements BookLoader.Loader
{
	private static final String[] suffixes = {"txt"};

	public String[] getSuffixes()
	{
		return suffixes;
	}

	private static String formatText(String txt)
	{
		return txt.replace("\r", "");
	}

	public BookContent load(VFile f) throws Exception
	{
		List<String> lines = new ArrayList<String>();
		String cs;

		InputStream fs = f.getInputStream();
		cs = BookLoader.detect(fs);
		fs.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(f.getInputStream(), cs));

		String line;
		while ((line = br.readLine()) != null)
			lines.add(formatText(line));
		return new PlainTextContent(lines);
	}

	public void unload(BookContent aBook)
	{
	}
}
