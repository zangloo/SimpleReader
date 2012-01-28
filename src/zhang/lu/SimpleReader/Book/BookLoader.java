package zhang.lu.SimpleReader.book;

import android.util.Log;
import org.xml.sax.SAXException;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.VFS.VFile;
import zhang.lu.SimpleReader.book.Haodoo.HaodooLoader;
import zhang.lu.SimpleReader.book.SRBOnline.SRBOnlineLoader;
import zhang.lu.SimpleReader.book.SimpleReader.SimpleReaderLoader;
import zhang.lu.SimpleReader.book.epub.EPubLoader;
import zhang.lu.SimpleReader.book.html.HtmlLoader;
import zhang.lu.SimpleReader.book.txt.TxtLoader;

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
	public interface Loader
	{
		boolean isBelong(VFile f);

		Book load(VFile file, Config.ReadingInfo ri) throws Exception;
	}

	private static List<Loader> loaders = new ArrayList<Loader>();
	private static Loader defaultLoader = null;

	private static void init()
	{
		defaultLoader = new TxtLoader();

		loaders.add(defaultLoader);

		loaders.add(new HtmlLoader());
		loaders.add(new HaodooLoader());
		loaders.add(new SimpleReaderLoader());
		loaders.add(new SRBOnlineLoader());
		try {
			System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
			loaders.add(new EPubLoader());
		} catch (SAXException e) {
			Log.e("BookLoader.init", e.getMessage());
		}
	}

	private static Loader findLoader(VFile f)
	{
		for (Loader l : loaders)
			if (l.isBelong(f))
				return l;
		return defaultLoader;
	}

	public static Book loadFile(String filePath, Config.ReadingInfo ri) throws Exception
	{
		VFile f = VFile.create(filePath);
		if (!f.exists())
			throw new FileNotFoundException();
		if (defaultLoader == null)
			init();
		Loader nl = findLoader(f);
		return nl.load(f, ri);
	}
}
