package zhang.lu.SimpleReader.Book;

import android.graphics.Bitmap;
import android.util.Log;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.Jsoup;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.XMLReaderFactory;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.VFS.RealFile;
import zhang.lu.SimpleReader.VFS.VFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubBookLoader extends XMLReaderAdapter implements BookLoader.Loader
{
	private static final String suffix = "epub";
	private static final String meta_file = "META-INF/container.xml";

	private static class EPubBook extends ChaptersBook
	{
		private BookContent content;
		private PlainTextContent ptc = new PlainTextContent();
		private ImageContent ic = new ImageContent();
		private final ZipFile zf;
		private final String ops_path;

		private EPubBook(ZipFile file, Config.ReadingInfo ri, ArrayList<TOCRecord> nps, String ops) throws Exception
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
				final NavPoint np = (NavPoint) TOC.get(index);

				final ZipArchiveEntry zae = zf.getEntry(ops_path + np.href);
				ArrayList<String> lines = new ArrayList<String>();
				ArrayList<String> imagerefs = new ArrayList<String>();

				InputStream is = zf.getInputStream(zae);
				String cs;
				cs = BookUtil.detect(is);
				is.close();

				is = zf.getInputStream(zae);
				BookUtil.HTML2Text(Jsoup.parse(is, cs, "").body(), lines, imagerefs);

				if (imagerefs.size() == 0) {
					ptc.setContent(lines);
					content = ptc;
				} else {
					ic.images.clear();
					for (String i : imagerefs) {
						Bitmap img = BookUtil.loadPicFromZip(zf, ops_path + i);
						if (img == null)
							Log.e("EPubBook.loadChapter", "Can not load image:" + ops_path + img);
						else
							ic.images.add(img);
					}
					if (ic.images.size() == 0)
						throw new Exception("Error load images:" + ops_path + np.href);
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

	// reading state enum
	private static enum RS
	{
		none, map, point, label, text
	}

	private static class NavPoint extends TOCRecord
	{
		final int order;
		final int level;
		String href = "";

		NavPoint(int o, int l)
		{
			super("");
			order = o;
			level = l;
		}

		@Override
		public int level() { return level; }
	}

	private static final String TAG_NAVMAP = "navmap";
	private static final String TAG_NAVPOINT = "navpoint";
	private static final String TAG_NAVLABEL = "navlabel";
	private static final String TAG_CONTENT = "content";
	private static final String TAG_TEXT = "text";

	private static final String ATTRIBUTE_PLAYORDER = "playOrder";

	private RS state;
	private int pi;
	private ArrayList<TOCRecord> nps;
	private final Stack<NavPoint> ps = new Stack<NavPoint>();

	public EPubBookLoader() throws SAXException
	{
		super(XMLReaderFactory.createXMLReader());
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		if (state == RS.text)
			ps.lastElement().title += new String(ch, start, length);
	}

	private void pushNP(final String o)
	{
		final int i = (o != null) ? Integer.parseInt(o) : pi++;
		ps.push(new NavPoint(i, ps.size()));
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
	{
		String tag = qName.toLowerCase().intern();
		switch (state) {
			case none:
				if (TAG_NAVMAP.equals(tag))
					state = RS.map;
				break;
			case map:
				if (TAG_NAVPOINT.equals(tag)) {
					pushNP(atts.getValue(ATTRIBUTE_PLAYORDER));
					state = RS.point;
				}
				break;
			case point:
				if (TAG_NAVPOINT.equals(tag))
					pushNP(atts.getValue(ATTRIBUTE_PLAYORDER));
				else if (TAG_NAVLABEL.equals(tag))
					state = RS.label;
				else if (TAG_CONTENT.equals(tag))
					if (!ps.isEmpty())
						ps.lastElement().href = atts.getValue("src");
				break;
			case label:
				if (TAG_TEXT.equals(tag)) {
					state = RS.text;
				}
				break;
			case text:
				break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		String tag = qName.toLowerCase().intern();
		switch (state) {
			case map:
				if (TAG_NAVMAP.equals(tag))
					state = RS.none;
				break;
			case point:
				if (TAG_NAVPOINT.equals(tag)) {
					final NavPoint np = ps.pop();
					if (np.title.length() == 0)
						np.title = "...";
					nps.add(np);
					state = (ps.isEmpty()) ? RS.map : RS.point;
				}
			case label:
				if (TAG_NAVLABEL.equals(tag))
					state = RS.point;
				break;
			case text:
				if (TAG_TEXT.equals(tag))
					state = RS.label;
				break;
			case none:
				break;
		}
	}

	@Override
	public void endDocument() throws SAXException
	{
		Collections.sort(nps, new Comparator<TOCRecord>()
		{
			public int compare(TOCRecord o1, TOCRecord o2)
			{
				return ((NavPoint) o1).order - ((NavPoint) o2).order;
			}
		});
	}

	public void dumpTOC()
	{
		Log.d("EPubBookLoader.dumpTOC", "+++++++++++++++++++++++");

		for (TOCRecord r : nps) {
			NavPoint np = (NavPoint) r;
			Log.d("EPubBookLoader.dumpTOC",
			      String.format("order = %d, level = %d, title = %s, href = %s", np.order, np.level,
					    np.title, np.href));
		}

		Log.d("EPubBookLoader.dumpTOC", "------------------------");
	}

	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	public Book load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		if (!RealFile.class.isInstance(file))
			throw new Exception("Cloud and zip based files are not supported, yet");

		ZipFile zf = new ZipFile(file.getRealPath());
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		// parser meta_file
		InputStream is = zf.getInputStream(zf.getEntry(meta_file));
		NodeList nl = db.parse(is).getDocumentElement().getElementsByTagName("rootfile");
		String opf_file = nl.item(0).getAttributes().getNamedItem("full-path").getNodeValue();
		String ops_path = opf_file.substring(0, opf_file.lastIndexOf('/') + 1);

		// parser opf_file
		is = zf.getInputStream(zf.getEntry(opf_file));
		nl = db.parse(is).getDocumentElement().getElementsByTagName("item");
		String ncx_file = null;
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i).getAttributes().getNamedItem("id").getNodeValue().equals("ncx")) {
				ncx_file = ops_path + nl.item(i).getAttributes().getNamedItem("href").getNodeValue();
				break;
			}

		if (ncx_file == null)
			throw new Exception("Error parser the ops file:\"" + file.getPath() + "\"");

		state = RS.none;
		pi = -65535;
		nps = new ArrayList<TOCRecord>();
		ps.clear();

		is = zf.getInputStream(zf.getEntry(ncx_file));
		parse(new InputSource(is));

		if (nps.isEmpty())
			throw new Exception("Error parser the ncx file:\"" + file.getPath() + "\"");
		if (ri.chapter >= nps.size())
			throw new Exception(
				String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

		dumpTOC();
		return new EPubBook(zf, ri, nps, ops_path);
	}
}
