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

public class TxtLoader extends Loader
{
	private static final String[] suffixes = {"txt"};

	@Override
	protected String[] getSuffixes()
	{
		return suffixes;
	}

	private static String formatText(String txt)
	{
		return txt.replace("\r", "");
	}

	@Override
	protected BookContent load(String filePath) throws Exception
	{
		VFile f = new VFile(filePath);
		List<String> lines = new ArrayList<String>();
		String cs;

		InputStream fs = f.getInputStream();
		cs = detect(fs);
		fs.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(f.getInputStream(), cs));

		String line;
		while ((line = br.readLine()) != null)
			lines.add(formatText(line));
		return new PlainTextContent(lines);
	}
}
