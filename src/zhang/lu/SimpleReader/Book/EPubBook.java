package zhang.lu.SimpleReader.Book;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.VFS.VFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubBook implements BookLoader.Loader
{
	private static final String suffix = "epub";

	private static class EPubBookContent extends PlainTextContent
	{
		private ArrayList<ChapterInfo> chapters = new ArrayList<ChapterInfo>();
		private int chapter;
		private List<TOCReference> tocrs;

		public EPubBookContent(VFile file, Config.ReadingInfo ri) throws IOException
		{
			Book book = (new EpubReader()).readEpub(file.getInputStream());
			chapters.clear();

			tocrs = book.getTableOfContents().getTocReferences();
			for (TOCReference tocr : tocrs)
				chapters.add(new ChapterInfo(tocr.getTitle()));

			loadChapter(ri.chapter);
			chapter = ri.chapter;
		}

		@Override
		public int getChapterCount()
		{
			return chapters.size();
		}

		@Override
		public String getChapterTitle(int index)
		{
			return chapters.get(index).title;
		}

		@Override
		public ArrayList<ChapterInfo> getChapterInfoList()
		{
			return chapters;
		}

		@Override
		public int getCurrChapter()
		{
			return chapter;
		}

		@Override
		protected boolean loadChapter(int index)
		{
			try {
				chapter = index;
				ArrayList<String> ls = new ArrayList<String>();
				Document doc = Jsoup.parse(tocrs.get(chapter).getResource().getInputStream(),
							   tocrs.get(chapter).getResource().getInputEncoding(), "");
				BookUtil.HTML2Text(doc.body(), ls);
				setContent(ls);
			} catch (Exception e) {
				ArrayList<String> list = new ArrayList<String>();
				list.add(e.getMessage());
				setContent(list);
			}
			return true;
		}
	}

	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	public BookContent load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		return new EPubBookContent(file, ri);
	}

	public void unload(BookContent aBook)
	{
		EPubBookContent book = (EPubBookContent) aBook;
		book.tocrs = null;
		book.chapters.clear();
	}

}
