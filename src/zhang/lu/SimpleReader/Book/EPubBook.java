package zhang.lu.SimpleReader.Book;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.SpineReference;
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

	private Book book;
	private List<SpineReference> srs;
	private ArrayList<String> titles = new ArrayList<String>();
	private int chapter;
	private String currChapterTitle = "";

	public String[] getSuffixes()
	{
		return suffixes;
	}

	public BookContent load(VFile file) throws Exception
	{
		book = (new EpubReader()).readEpub(new FileInputStream(file));
		srs = book.getSpine().getSpineReferences();
		titles.clear();

		for (SpineReference sr : srs)
			titles.add(sr.getResource().getHref());
		chapter = 0;

		loadChapter();
		return this;
	}

	public void unload(BookContent aBook)
	{
		book = null;
		srs = null;
		titles.clear();
	}

	public int getChapterCount()
	{
		return titles.size();
	}

	public String getChapterTitle()
	{
		// epublib cannot get actual title, so use first line instead
		return currChapterTitle;
	}

	public String getChapterTitle(int index)
	{
		return titles.get(index);
	}

	public ArrayList<String> getChapterTitleList()
	{
		return titles;
	}

	public int getCurrChapter()
	{
		return chapter;
	}

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
			Document doc = Jsoup.parse(srs.get(chapter).getResource().getInputStream(),
						   srs.get(chapter).getResource().getInputEncoding(), "");
			currChapterTitle = doc.title();
			BookUtil.HTML2Text(doc.body(), ls);
			setContent(ls);
		} catch (Exception e) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getMessage());
			setContent(list);
		}
	}
}
