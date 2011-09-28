package zhang.lu.SimpleReader.Book;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-28
 * Time: 上午11:47
 */
public class BookUtil
{
	public static final String defaultCNEncode = "GBK";
	public static final String cnEncodePrefix = "GB";

	public static final int detectFileReadBlockSize = 2048;
	public static byte[] detectFileReadBuffer = new byte[detectFileReadBlockSize];

	static public void HTML2Text(Element node, List<String> lines)
	{
		for (Node child : node.childNodes()) {
			if (child instanceof TextNode) {
				String t = ((TextNode) child).text();
				if (t.trim().length() > 0)
					lines.add(t);
			} else if (child instanceof Element)
				HTML2Text((Element) child, lines);
		}
	}

	static String detect(InputStream is)
	{
		UniversalDetector detector = new UniversalDetector(null);

		int len;
		try {
			while ((len = is.read(detectFileReadBuffer)) != -1) {
				detector.handleData(detectFileReadBuffer, 0, len);
				if (detector.isDone())
					break;
			}
		} catch (IOException e) {
			return defaultCNEncode;
		}

		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		detector.reset();

		if (encoding == null)
			return defaultCNEncode;
		if (encoding.indexOf(cnEncodePrefix) == 0)
			encoding = defaultCNEncode;

		return encoding;
	}
}
