package net.lzrj.SimpleReader.book;

import android.content.Context;
import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.R;
import net.lzrj.SimpleReader.book.SRBOnline.SRBOnlineLoader;
import net.lzrj.SimpleReader.book.SimpleReader.SimpleReaderLoader;
import net.lzrj.SimpleReader.book.epub.EPubLoader;
import net.lzrj.SimpleReader.book.haodoo.HaodooLoader;
import net.lzrj.SimpleReader.book.html.HtmlLoader;
import net.lzrj.SimpleReader.book.pdf.PDFLoader;
import net.lzrj.SimpleReader.book.txt.TxtLoader;
import net.lzrj.SimpleReader.vfs.VFile;

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
	private static List<Loader> loaders = new ArrayList<Loader>();

	private static void init()
	{
		loaders.add(new TxtLoader());
		loaders.add(new HtmlLoader());
		loaders.add(new HaodooLoader());
		loaders.add(new SimpleReaderLoader());
		loaders.add(new SRBOnlineLoader());
		loaders.add(new PDFLoader());
		loaders.add(new EPubLoader());
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

	public interface Loader
	{
		boolean isBelong(VFile f);

		Book load(VFile file, Config.ReadingInfo ri) throws Exception;
	}
}
