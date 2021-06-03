package com.lingzeng.SimpleReader.book.epub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.lingzeng.SimpleReader.ContentImage;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.HtmlContentNodeCallback;
import com.lingzeng.SimpleReader.UString;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

public class EPubContentNodeCallback implements HtmlContentNodeCallback
{
	private final Book book;
	private final String htmlPath;
	private final String basePath;
	private String fragmentId = null;
	private boolean found = false;
	private boolean pass = false;

	public EPubContentNodeCallback(Book book, String htmlPath, String fragmentId)
	{
		this.book = book;
		this.htmlPath = htmlPath;
		int pos = htmlPath.lastIndexOf('/');
		if (pos < 0)
			pos = 0;
		basePath = htmlPath.substring(0, pos);
		if (fragmentId != null && fragmentId.length() > 0)
			this.fragmentId = fragmentId;
	}

	@Override
	public void process(Element element)
	{
		if (fragmentId == null) return;
		if (pass) return;
		if (!element.tagName().equalsIgnoreCase("span")) return;
		String newId = element.id();
		if (newId.equals(fragmentId))
			found = true;
		else if (found)
			if (book.getResources().getByHref(htmlPath + "#" + newId) != null)
				pass = true;
	}

	@Override
	public void addText(List<ContentLine> lines, String text)
	{
		if (fragmentId == null || found)
			lines.add(new UString(text));
	}

	@Override
	public void addImage(List<ContentLine> lines, String src)
	{
		if (fragmentId == null || found)
			try {
				String ref = FilenameUtils.concat(basePath, src);
				Resource href = book.getResources().getByHref(ref);
				lines.add(new EPubImageLine(href == null ? null : href.getData()));
			} catch (IOException e) {
				lines.add(new EPubImageLine(null));
			}
	}

	private static class EPubImageLine extends ContentImage
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
