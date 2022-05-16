package net.lzrj.SimpleReader.book.epub;

import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.*;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import org.jsoup.Jsoup;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午8:39
 */
class EPubBook extends ChaptersBook
{
	private final Book book;
	private final ContentBase content = new ContentBase();

	EPubBook(Book book, Config.ReadingInfo ri, ArrayList<TOCRecord> toc)
	{
		TOC = toc;
		this.book = book;
		loadChapter(ri.chapter);
	}

	@Override
	protected boolean loadChapter(int index)
	{
		try {
			chapter = index;
			TOCReference ref = ((EPubLoader.EPubTOC) TOC.get(index)).ref;
			Resource resource = ref.getResource();

			String charset = BookUtil.detect(resource.getInputStream());

			final String htmlPath = resource.getHref();
			ArrayList<ContentLine> lines = new ArrayList<>();
			BookUtil.HTML2Text(Jsoup.parse(resource.getInputStream(), charset, "").body(), lines,
				new EPubContentNodeCallback(book, htmlPath, ref.getFragmentId()));
			if (lines.size() == 0)
				lines.add(new UString(" "));
			content.setContent(lines);
		} catch (Exception e) {
			e.printStackTrace();
			ArrayList<ContentLine> list = new ArrayList<>();
			String message = e.getMessage();
			if (message == null)
				message = e.toString();
			list.add(new UString(message));
			content.setContent(list);
		}
		return true;
	}

	@Override
	public Content content(int index)
	{
		return content;
	}

	@Override
	public void close()
	{
	}
}

