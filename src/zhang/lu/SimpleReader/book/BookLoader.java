package zhang.lu.SimpleReader.book;

import android.content.Context;
import android.util.Log;
import org.xml.sax.SAXException;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.R;
import zhang.lu.SimpleReader.book.SRBOnline.SRBOnlineLoader;
import zhang.lu.SimpleReader.book.SimpleReader.SimpleReaderLoader;
import zhang.lu.SimpleReader.book.epub.EPubLoader;
import zhang.lu.SimpleReader.book.haodoo.HaodooLoader;
import zhang.lu.SimpleReader.book.html.HtmlLoader;
import zhang.lu.SimpleReader.book.txt.TxtLoader;
import zhang.lu.SimpleReader.vfs.VFile;

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

	private static void init()
	{
		loaders.add(new TxtLoader());
		loaders.add(new HtmlLoader());
		loaders.add(new HaodooLoader());
		loaders.add(new SimpleReaderLoader());
		loaders.add(new SRBOnlineLoader());
		try {
			System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
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
		return null;
	}

	public static Book loadFile(Context context, String filePath, Config.ReadingInfo ri) throws Exception
	{
		VFile f = VFile.create(filePath);
		if (!f.exists())
			throw new FileNotFoundException();
		if (loaders.size() == 0)
			init();
		Loader nl = findLoader(f);
		if (nl == null)
			throw new Exception(context.getString(R.string.file_not_supported));
		return nl.load(f, ri);
	}
}
