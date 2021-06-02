package com.lingzeng.SimpleReader.book.epub;

import android.graphics.Bitmap;
import android.util.Log;
import com.lingzeng.SimpleReader.*;
import com.lingzeng.SimpleReader.book.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午8:39
 */
class EPubBook extends ChaptersBook
{
	private final ZipFile zf;
	private final String ops_path;
	private final ContentBase content = new ContentBase();

	EPubBook(ZipFile file, Config.ReadingInfo ri, ArrayList<TOCRecord> nps, String ops)
	{
		ops_path = ops;
		zf = file;
		TOC = nps;
		loadChapter(ri.chapter);
	}

	@Override
	protected boolean loadChapter(int index)
	{
		try {
			chapter = index;
			final EPubLoader.NavPoint np = (EPubLoader.NavPoint) TOC.get(index);

			ZipArchiveEntry zae;
			ArrayList<ContentLine> lines = new ArrayList<>();
			for (String href : np.href) {
				final String htmlPath = ops_path + href;
				int pos = htmlPath.lastIndexOf('/');
				if (pos < 0)
					pos = 0;
				final String path = htmlPath.substring(0, pos);

				zae = zf.getEntry(htmlPath);

				InputStream is = zf.getInputStream(zae);
				String cs;
				cs = BookUtil.detect(is);
				is.close();

				is = zf.getInputStream(zae);
				BookUtil.HTML2Text(Jsoup.parse(is, cs, "").body(), lines, new ContentImageLoader()
				{
					@Override
					public ContentImage loadImage(String src)
					{
						return new EPubImageLine(BookUtil.concatPath(path, src));
					}
				});
			}
			content.setContent(lines);
		} catch (Exception e) {
			ArrayList<ContentLine> list = new ArrayList<>();
			list.add(new UString(e.getMessage()));
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

	private class EPubImageLine extends ContentImage
	{
		private Bitmap image;

		public EPubImageLine(String ref)
		{
			super(ref);
		}

		@Override
		public Bitmap getImage()
		{
			if (image == null) {
				image = BookUtil.loadPicFromZip(zf, ref);
			}
			if (image == null)
				Log.e("EPubImageLine#getImage", "Can not load image:" + ref);
			return image;
		}

		@Override
		public boolean isImage()
		{
			return true;
		}
	}
}

