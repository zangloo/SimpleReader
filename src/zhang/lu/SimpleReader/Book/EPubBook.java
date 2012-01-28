package zhang.lu.SimpleReader.Book;

import android.graphics.Bitmap;
import android.util.Log;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.Jsoup;
import zhang.lu.SimpleReader.Config;

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
	private BookContent content;
	private PlainTextContent ptc = new PlainTextContent();
	private EPubImageContent ic;
	private final ZipFile zf;
	private final String ops_path;

	private class EPubImageContent extends ImageContent
	{
		ArrayList<String> imgref = new ArrayList<String>();
		HashMap<Integer, Bitmap> images = new HashMap<Integer, Bitmap>();

		@Override
		public int imageCount()
		{
			return imgref.size();
		}

		@Override
		public Bitmap image(int index)
		{
			if (images.containsKey(index))
				return images.get(index);
			Bitmap bm = BookUtil.loadPicFromZip(zf, ops_path + imgref.get(index));
			images.put(index, bm);

			if (bm == null)
				Log.e("EPubImageContent.image", "Can not load image:" + imgref.get(index));
			return bm;
		}
	}

	EPubBook(ZipFile file, Config.ReadingInfo ri, ArrayList<TOCRecord> nps, String ops) throws Exception
	{
		ops_path = ops;
		zf = file;
		TOC = nps;
		ic = new EPubImageContent();
		loadChapter(ri.chapter);
	}

	@Override
	protected boolean loadChapter(int index)
	{
		try {
			chapter = index;
			final EPubBookLoader.NavPoint np = (EPubBookLoader.NavPoint) TOC.get(index);

			final ZipArchiveEntry zae = zf.getEntry(ops_path + np.href);
			ArrayList<String> lines = new ArrayList<String>();

			InputStream is = zf.getInputStream(zae);
			String cs;
			cs = BookUtil.detect(is);
			is.close();

			is = zf.getInputStream(zae);
			ic.imgref.clear();
			BookUtil.HTML2Text(Jsoup.parse(is, cs, "").body(), lines, ic.imgref);

			if (ic.imgref.size() == 0) {
				ptc.setContent(lines);
				content = ptc;
			} else {
				ic.images.clear();
				content = ic;
			}
		} catch (Exception e) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getMessage());
			ptc.setContent(list);
			content = ptc;
		}
		return true;
	}

	@Override
	public BookContent getContent(int index) { return content; }

	@Override
	public void close() {}
}

