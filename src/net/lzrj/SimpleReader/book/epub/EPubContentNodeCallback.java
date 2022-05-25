package net.lzrj.SimpleReader.book.epub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.HtmlContentNodeCallback;
import net.lzrj.SimpleReader.ImageContent;
import net.lzrj.SimpleReader.book.BookUtil;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

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
	public ContentLine createImage(List<ContentLine> lines, String src)
	{
		String ref = FilenameUtils.concat(basePath, src);
		Resource href = book.getResources().getByHref(ref);
		try {
			return new EPubImageLine(href == null ? null : href.getData());
		} catch (IOException e) {
			return new EPubImageLine(null);
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

	private static class EPubImageLine extends ImageContent
	{
		private final byte[] bytes;
		private Bitmap image;

		public EPubImageLine(byte[] bytes)
		{
			this.bytes = bytes;
		}

		@Override
		public Bitmap getImage()
		{
			if (bytes == null)
				return null;
			if (image == null)
				image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			return image;
		}

		@Override
		public boolean isImage()
		{
			return true;
		}
	}
}
