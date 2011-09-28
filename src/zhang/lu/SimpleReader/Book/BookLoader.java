package zhang.lu.SimpleReader.Book;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:01
 */
public class BookLoader
{
	interface Loader
	{
		String[] getSuffixes();

		BookContent load(VFile file) throws Exception;

		void unload(BookContent aBook);
	}

	private static List<Loader> loaders = new ArrayList<Loader>();
	private static Loader defaultLoader = null;

	private static BookContent book = null;
	private static Loader currLoader = null;

	private static void init()
	{
		defaultLoader = new TxtLoader();

		loaders.add(defaultLoader);

		loaders.add(new HtmlLoader());
		loaders.add(new HaodooLoader());
		loaders.add(new SimpleReaderBook());
		loaders.add(new EPubBook());
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

	public static void unloadBook()
	{
		if ((currLoader != null) && (book != null)) {
			currLoader.unload(book);
			currLoader = null;
			book = null;
		}
	}
}
