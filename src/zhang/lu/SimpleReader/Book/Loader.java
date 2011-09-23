package zhang.lu.SimpleReader.Book;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:01
 */
public abstract class Loader
{
	private static List<Loader> loaders = new ArrayList<Loader>();
	private static Loader defaultLoader = null;

	public static final String defaultCNEncode = "GBK";
	public static final String cnEncodePrefix = "GB";

	public static final int detectFileReadBlockSize = 2048;
	public static byte[] detectFileReadBuffer = new byte[detectFileReadBlockSize];

	private static BookContent book = null;
	private static Loader currLoader = null;

	private static void init()
	{
		defaultLoader = new TxtLoader();

		loaders.add(defaultLoader);

		loaders.add(new HtmlLoader());
		loaders.add(new HaodooLoader());
		loaders.add(new SimpleReaderBook());
	}

	private static Loader findLoader(String filePath)
	{
		for (Loader l : loaders)
			for (String s : l.getSuffixes())
				if (filePath.toLowerCase().endsWith("." + s))
					return l;
		return defaultLoader;
	}

	public static BookContent loadFile(String filePath)
	{
		unloadBook();
		VFile f = new VFile(filePath);
		if (!f.exists())
			return null;
		try {
			if (defaultLoader == null)
				init();
			currLoader = findLoader(filePath);
			return book = currLoader.load(f);
		} catch (Exception e) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getMessage());
			return book = new PlainTextContent(list);
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

	public static void unloadBook()
	{
		if ((currLoader != null) && (book != null)) {
			currLoader.unload(book);
			currLoader = null;
			book = null;
		}
	}

	protected abstract String[] getSuffixes();

	protected abstract BookContent load(VFile file) throws Exception;

	protected abstract void unload(BookContent aBook);
}
