package zhang.lu.SimpleReader.Book;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubBook extends PlainTextContent implements BookLoader.Loader
{
	private static final String[] suffixes = {"epub"};

	private ArrayList<String> titles = new ArrayList<String>();
	private int chapter;
	private List<TOCReference> tocrs;

	public String[] getSuffixes()
	{
		return suffixes;
	}

	public BookContent load(VFile file) throws Exception
	{
		Book book = (new EpubReader()).readEpub(new FileInputStream(file));
		titles.clear();

		tocrs = book.getTableOfContents().getTocReferences();
		for (TOCReference tocr : tocrs)
			titles.add(tocr.getTitle());
		chapter = 0;

		loadChapter();
		return this;
	}

	public void unload(BookContent aBook)
	{
		tocrs = null;
		titles.clear();
	}

	@Override
	public int getChapterCount()
	{
		return titles.size();
	}

	@Override
	public String getChapterTitle(int index)
	{
		return titles.get(index);
	}

	@Override
	public ArrayList<String> getChapterTitleList()
	{
		return titles;
	}

	@Override
	public int getCurrChapter()
	{
		return chapter;
	}

	@Override
	public boolean gotoChapter(int index)
	{
		if ((index < 0) || (index >= titles.size()))
			return false;
		chapter = index;
		loadChapter();
		return true;
	}

	private void loadChapter()
	{
		try {
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
	}
}
