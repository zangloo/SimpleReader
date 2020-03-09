package com.lingzeng.SimpleReader.book.epub;

import android.graphics.Bitmap;
import android.util.Log;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.Jsoup;
import com.lingzeng.SimpleReader.Config;
import com.lingzeng.SimpleReader.UString;
import com.lingzeng.SimpleReader.book.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

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
	private EPubImageContent content = new EPubImageContent();

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
			ArrayList<UString> lines = new ArrayList<UString>();
			int start = 0;
			content.imageCount = 0;
			for (String href : np.href) {
				String htmlPath = ops_path + href;
				zae = zf.getEntry(htmlPath);
				LinkedHashSet<String> imgref = new LinkedHashSet<String>();

				InputStream is = zf.getInputStream(zae);
				String cs;
				cs = BookUtil.detect(is);
				is.close();

				is = zf.getInputStream(zae);
				BookUtil.HTML2Text(Jsoup.parse(is, cs, "").body(), lines, imgref);

				content.imageCount += imgref.size();
				if (imgref.size() > 0) {
					int pos = htmlPath.lastIndexOf('/');
					if (pos < 0)
						pos = 0;
					String path = htmlPath.substring(0, pos);
					List<UString> realRef = new ArrayList<UString>(imgref.size());
					for (String ref : imgref)
						realRef.add(new UString(BookUtil.concatPath(path, ref)));

					lines.addAll(start, realRef);
					content.images.clear();
				}
				start += lines.size();
			}
			content.setContent(lines);
		} catch (Exception e) {
			ArrayList<UString> list = new ArrayList<UString>();
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

	private class EPubImageContent extends PlainTextContent
	{
		HashMap<Integer, Bitmap> images = new HashMap<Integer, Bitmap>();
		int imageCount = 0;

		@Override
		public int imageCount()
		{
			return imageCount;
		}

		@Override
		public Bitmap image(int index)
		{
			if (images.containsKey(index))
				return images.get(index);
			Bitmap bm = BookUtil.loadPicFromZip(zf, line(index).toString());
			images.put(index, bm);

			if (bm == null)
				Log.e("EPubImageContent.image", "Can not load image:" + line(index));
			return bm;
		}
	}
}

