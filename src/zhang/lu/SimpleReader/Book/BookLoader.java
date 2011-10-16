package zhang.lu.SimpleReader.Book;

import zhang.lu.SimpleReader.VFS.VFile;

import java.io.FileNotFoundException;
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
		boolean isBelong(VFile f);

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

	private static Loader findLoader(VFile f)
	{
		for (Loader l : loaders)
			if (l.isBelong(f))
				return l;
		return defaultLoader;
	}

	public static BookContent loadFile(String filePath) throws Exception
	{
		unloadBook();
		VFile f = VFile.create(filePath);
		if (!f.exists())
			throw new FileNotFoundException();
		if (defaultLoader == null)
			init();
		currLoader = findLoader(f);
		return book = currLoader.load(f);
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
