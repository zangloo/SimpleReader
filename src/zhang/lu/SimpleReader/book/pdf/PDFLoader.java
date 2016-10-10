package zhang.lu.SimpleReader.book.pdf;

import android.util.SparseArray;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.SimpleBookmark;
import com.itextpdf.text.pdf.parser.*;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.UString;
import zhang.lu.SimpleReader.book.*;
import zhang.lu.SimpleReader.vfs.VFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zang.loo
 * Date: 16-10-7
 * Time: 上午11:39
 */
public class PDFLoader extends ChaptersBook implements BookLoader.Loader
{
	private static final String suffix = "pdf";

	private PdfReader reader;
	private List<Integer> chapters;
	private SparseArray<List<UString>> contents;

	@Override
	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	@Override
	public Book load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		return initBook(file.getRealPath());
	}

	private Book initBook(String path) throws IOException
	{
		reader = new PdfReader(path);
		List<HashMap<String, Object>> bookmarks = SimpleBookmark.getBookmark(reader);
		chapters = new ArrayList<Integer>(bookmarks.size());
		contents = new SparseArray<List<UString>>(bookmarks.size());
		TOC.clear();
		// only support top level
		for (HashMap<String, Object> bm : bookmarks) {
			String page = ((String) bm.get("Page")).split(" ")[0];
			chapters.add(Integer.valueOf(page));
			TOC.add(new TOCRecord((String) bm.get("Title")));
		}
		// for last chapter's last page calc
		chapters.add(reader.getNumberOfPages());
		return this;
	}

	@Override
	public Content content(int index)
	{
		List<UString> lines;
		try {
			lines = contents.get(index);
			if (lines == null) {
				lines = new ArrayList<UString>();
				contents.put(index, lines);
				for (int i = chapters.get(index); i < chapters.get(index + 1); i++) {
					Rectangle rect = reader.getPageSize(i);
					RenderFilter regionFilter = new RegionTextRenderFilter(rect);
					TextExtractionStrategy strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), regionFilter);
					String text = PdfTextExtractor.getTextFromPage(reader, i, strategy);
					if (text == null)
						text = "";
					String[] data = text.split("\n");
					for (String line : data)
						lines.add(new UString(line));
				}
			}
		} catch (Exception e) {
			lines = new ArrayList<UString>();
			lines.add(new UString(e.getMessage()));
		}
		return new PlainTextContent(lines);
	}

	@Override
	public void close()
	{
	}

	@Override
	protected boolean loadChapter(int index)
	{
		if (index > TOC.size())
			return false;
		chapter = index;
		return true;
	}
}
