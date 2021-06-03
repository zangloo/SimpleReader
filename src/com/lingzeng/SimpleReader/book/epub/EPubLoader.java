package com.lingzeng.SimpleReader.book.epub;

import com.lingzeng.SimpleReader.Config;
import com.lingzeng.SimpleReader.book.BookLoader;
import com.lingzeng.SimpleReader.book.TOCRecord;
import com.lingzeng.SimpleReader.vfs.RealFile;
import com.lingzeng.SimpleReader.vfs.VFile;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubLoader implements BookLoader.Loader
{
	private static final String suffix = "epub";
	private static final String meta_file = "META-INF/container.xml";

	public EPubLoader()
	{
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
	}

	@Override
	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	@Override
	public EPubBook load(VFile file, Config.ReadingInfo ri) throws Exception
	{

		if (!(file instanceof RealFile))
			throw new Exception("Cloud and zip based files are not supported, yet");

		EpubReader reader = new EpubReader();
		Book book;
		try (InputStream is = new FileInputStream(file.getRealPath())) {
			book = reader.readEpub(is);
		}

		if (ri.chapter >= book.getTableOfContents().size())
			throw new Exception(
				String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

		ArrayList<TOCRecord> toc = new ArrayList<>();
		setupTOC(book.getTableOfContents().getTocReferences(), toc, 0);

		return new EPubBook(book, ri, toc);
	}

	private void setupTOC(List<TOCReference> references, ArrayList<TOCRecord> toc, int level)
	{
		for (TOCReference ref : references) {
			toc.add(new EPubTOC(ref, level));
			List<TOCReference> children = ref.getChildren();
			if (children != null)
				setupTOC(children, toc, level + 1);
		}
	}

	static class EPubTOC extends TOCRecord
	{
		final TOCReference ref;
		final int level;

		EPubTOC(TOCReference ref, int level)
		{
			super(ref.getTitle());
			this.ref = ref;
			this.level = level;
		}

		@Override
		public int level()
		{
			return level;
		}
	}
}
