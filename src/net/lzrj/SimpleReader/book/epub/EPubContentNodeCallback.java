package net.lzrj.SimpleReader.book.epub;

import net.lzrj.SimpleReader.HtmlContentNodeCallback;
import net.lzrj.SimpleReader.UString;
import net.lzrj.SimpleReader.book.BookUtil;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;

public class EPubContentNodeCallback implements HtmlContentNodeCallback
{
	private final Book book;
	private final String basePath;

	public EPubContentNodeCallback(Book book, String htmlPath)
	{
		this.book = book;
		int pos = htmlPath.lastIndexOf('/');
		if (pos < 0)
			pos = 0;
		basePath = htmlPath.substring(0, pos);
	}

	@Override
	public UString.ImageValue imageValue(String src)
	{
		String ref = FilenameUtils.concat(basePath, src);
		Resource href = book.getResources().getByHref(ref);
		if (href == null)
			return null;
		try {
			return new UString.ImageValue(ref, href.getData());
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String getCss(String href) throws IOException
	{
		String absoluteHref = BookUtil.concatPath(basePath, href);
		Resource resource = book.getResources().getByHref(absoluteHref);
		if (resource == null)
			return null;
		Reader reader = resource.getReader();
		return IOUtils.toString(reader);
	}

}
