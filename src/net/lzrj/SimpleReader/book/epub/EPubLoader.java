package net.lzrj.SimpleReader.book.epub;

import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.book.BookLoader;
import net.lzrj.SimpleReader.book.TOCRecord;
import net.lzrj.SimpleReader.vfs.RealFile;
import net.lzrj.SimpleReader.vfs.VFile;
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

		int chapter_count = book.getSpine().size();
		if (ri.chapter >= chapter_count)
			throw new Exception(
				String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

		ArrayList<TOCRecord> toc = new ArrayList<>();
		setupTOC(book.getTableOfContents().getTocReferences(), toc, 0);

		int chapter_index = 0;
		for (TOCRecord entry : toc) {
			EPubTOC et = (EPubTOC) entry;
			String src_file = et.ref.getResourceId();
			for (int i = chapter_index; i < chapter_count; i++) {
				String chapter_href = book.getSpine().getSpineReferences().get(i).getResourceId();
				if (chapter_href != null && chapter_href.equals(src_file)){
					et.first_chapter_index = i;
					chapter_index = i;
					break;
				}
			}
		}

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
		private int first_chapter_index = 0;

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

		int first_chapter_index()
		{
			return first_chapter_index;
		}
	}
}
