package zhang.lu.SimpleReader.book.epub;

import android.graphics.Bitmap;
import android.util.Log;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.Jsoup;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.book.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 下午8:39
 */
class EPubBook extends ChaptersBook
{
	private EPubImageContent content = new EPubImageContent();
	private final ZipFile zf;
	private final String ops_path;

	private class EPubImageContent extends PlainTextContent
	{
		HashMap<Integer, Bitmap> images = new HashMap<Integer, Bitmap>();
		int imageCount = 0;

		@Override
		public int imageCount() {return imageCount;}

		@Override
		public Bitmap image(int index)
		{
			if (images.containsKey(index))
				return images.get(index);
			Bitmap bm = BookUtil.loadPicFromZip(zf, ops_path + line(index));
			images.put(index, bm);

			if (bm == null)
				Log.e("EPubImageContent.image", "Can not load image:" + line(index));
			return bm;
		}
	}

	EPubBook(ZipFile file, Config.ReadingInfo ri, ArrayList<TOCRecord> nps, String ops) throws Exception
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

			final ZipArchiveEntry zae = zf.getEntry(ops_path + np.href);
			ArrayList<String> lines = new ArrayList<String>();
			ArrayList<String> imgref = new ArrayList<String>();

			InputStream is = zf.getInputStream(zae);
			String cs;
			cs = BookUtil.detect(is);
			is.close();

			is = zf.getInputStream(zae);
			BookUtil.HTML2Text(Jsoup.parse(is, cs, "").body(), lines, imgref);

			content.imageCount = imgref.size();
			if (content.imageCount > 0) {
				lines.addAll(0, imgref);
				content.images.clear();
			}
			content.setContent(lines);
		} catch (Exception e) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getMessage());
			content.setContent(list);
		}
		return true;
	}

	@Override
	public Content getContent(int index) { return content; }

	@Override
	public void close() {}
}

