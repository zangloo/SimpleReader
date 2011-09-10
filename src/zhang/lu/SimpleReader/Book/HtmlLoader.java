package zhang.lu.SimpleReader.Book;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:23
 */

public class HtmlLoader extends Loader
{
	private static final String[] suffixes = {"htm", "html"};

	@Override
	protected String[] getSuffixes()
	{
		return suffixes;
	}

	static private void HTMLText(Element node, List<String> lines)
	{
		for (Node child : node.childNodes()) {
			if (child instanceof TextNode) {
				String t = ((TextNode) child).text();
				if (t.trim().length() > 0)
					lines.add(t);
			} else if (child instanceof Element)
				HTMLText((Element) child, lines);
		}
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

		HTMLText(Jsoup.parse(f.getInputStream(), cs, "").body(), lines);
		return new PlainTextContent(lines);
	}
}
