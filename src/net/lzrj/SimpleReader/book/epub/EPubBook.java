package net.lzrj.SimpleReader.book.epub;

import net.lzrj.SimpleReader.Config;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.*;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午8:39
 */
class EPubBook extends ChaptersBook
{
	private final Book book;
	private final Map<Integer, EPubChapter> chapters = new HashMap<>();

	EPubBook(Book book, Config.ReadingInfo ri, ArrayList<TOCRecord> toc)
	{
		TOC = toc;
		this.book = book;
		loadChapter(ri.chapter);
	}

	@Override
	protected boolean loadChapter(int index)
	{
		this.chapter = index;
		if (chapters.get(index) != null)
			return true;
		EPubChapter chapter;
		try {
			Resource resource = book.getSpine().getSpineReferences().get(index).getResource();

			String charset = BookUtil.detectCharset(resource.getInputStream());

			final String htmlPath = resource.getHref();
			Document document = Jsoup.parse(resource.getInputStream(), charset, "");
			BookUtil.HtmlContent htmlContent = BookUtil.HTML2Text(document, new EPubContentNodeCallback(book, htmlPath));
			List<UString> lines = htmlContent.lines;
			if (lines.size() == 0)
				lines.add(new UString(" "));
			chapter = new EPubChapter(htmlPath, new ContentBase(lines), htmlContent.fragmentMap.size() == 0 ? null : htmlContent.fragmentMap);
		} catch (Exception e) {
			e.printStackTrace();
			ArrayList<UString> list = new ArrayList<>();
			String message = e.getMessage();
			if (message == null)
				message = e.toString();
			list.add(new UString(message));
			chapter = new EPubChapter("_" + index, new ContentBase(list), null);
		}
		chapters.put(index, chapter);
		return true;
	}

	@Override
	public Content content(int index)
	{
		return chapters.get(index).content;
	}

	@Override
	public int chapterCount()
	{
		return book.getSpine().size();
	}

	@Override
	public String readingTitle(int chapter, int line, int offset)
	{
		int tocIndex = tocIndex(chapter, line, offset);
		return TOC.get(tocIndex).title;
	}

	@Override
	public int tocIndex(int chapterIndex, int line, int offset)
	{
		loadChapter(chapterIndex);
		EPubChapter chapter = chapters.get(chapterIndex);
		LinkedHashMap<String, Content.Position> fragmentMap = chapter.fragmentMap;
		Map.Entry<String, Content.Position> last = null;
		if (fragmentMap != null)
			for (Map.Entry<String, Content.Position> next : fragmentMap.entrySet()) {
				Content.Position position = next.getValue();
				if (line < position.line || (line == position.line && offset < position.offset))
					break;
				last = next;
			}
		SpineReference spineReference = book.getSpine().getSpineReferences().get(chapterIndex);
		int tocSize = TOC.size();
		int matchedToc = tocSize - 1;
		for (int i = 0; i < tocSize; i++) {
			TOCReference tocReference = ((EPubLoader.EPubTOC) TOC.get(i)).ref;
			String resourceId = tocReference.getResourceId();
			if (resourceId == null) {
				if (i == chapterIndex)
					return i;
			} else if (spineReference.getResourceId().equals(resourceId)) {
				if (last == null)
					return i;
				else if (last.getKey().equals(tocReference.getFragmentId()))
					return i;
				matchedToc = i;
			}
		}
		return matchedToc;
	}

	@Override
	public Content.Position gotoToc(int tocIndex)
	{
		TOCReference tocReference = ((EPubLoader.EPubTOC) TOC.get(tocIndex)).ref;
		String resourceId = tocReference.getResourceId();
		if (resourceId == null) {
			loadChapter(tocIndex);
			return new Content.Position(0, 0);
		}
		List<SpineReference> spineReferences = book.getSpine().getSpineReferences();
		for (int i = 0; i < spineReferences.size(); i++)
			if (resourceId.equals(spineReferences.get(i).getResourceId())) {
				loadChapter(i);
				EPubChapter chapter = chapters.get(i);
				Content.Position position = chapter.fragmentMap.get(tocReference.getFragmentId());
				if (position == null)
					return new Content.Position(0, 0);
				else
					return position;
			}
		return null;
	}

	@Override
	public Content.Position gotoLink( String href)
	{
		String[] parts = href.split("#");
		EPubChapter chapter = null;
		if (parts[0].length() > 0) {
			EPubChapter current = chapters.get(this.chapter);
			if (current == null)
				return null;
			String prefix = BookUtil.getParentPath(current.path);
			String path;
			if (prefix == null)
				path = parts[0];
			else
				path = BookUtil.concatPath(prefix, parts[0]);
			List<SpineReference> spineReferences = book.getSpine().getSpineReferences();
			for (int i = 0; i < spineReferences.size(); i++)
				if (path.equals(spineReferences.get(i).getResource().getHref())) {
					loadChapter(i);
					chapter = chapters.get(i);
				}
		} else
			chapter = chapters.get(this.chapter);

		if (chapter == null)
			return null;

		if (parts.length > 1 && parts[1].length() > 0) {
			Content.Position position = chapter.fragmentMap.get(parts[1]);
			if (position == null)
				return new Content.Position(0, 0);
			else
				return position;
		} else {
			return new Content.Position(0, 0);
		}
	}
}

