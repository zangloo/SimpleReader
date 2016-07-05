package zhang.lu.SimpleReader.book.epub;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.XMLReaderFactory;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.book.BookLoader;
import zhang.lu.SimpleReader.book.TOCRecord;
import zhang.lu.SimpleReader.vfs.RealFile;
import zhang.lu.SimpleReader.vfs.VFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubLoader extends XMLReaderAdapter implements BookLoader.Loader
{
	private static final String suffix = "epub";
	private static final String meta_file = "META-INF/container.xml";

	// reading state enum
	private static enum RS
	{
		none, map, point, label, text
	}

	static class NavPoint extends TOCRecord
	{
		final int order;
		final int level;
		List<String> href = new ArrayList<String>();

		NavPoint(int o, int l)
		{
			super("");
			order = o;
			level = l;
		}

		@Override
		public int level()
		{
			return level;
		}
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

	public EPubLoader() throws SAXException
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
						ps.lastElement().href.add(atts.getValue("src"));
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

	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	public zhang.lu.SimpleReader.book.Book load(VFile file, Config.ReadingInfo ri) throws Exception
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
		Document opf = db.parse(is);

		List<String> spines = new ArrayList<String>();
		nl = opf.getElementsByTagName("itemref");
		for (int i = 0; i < nl.getLength(); i++) {
			String idref = nl.item(i).getAttributes().getNamedItem("idref").getNodeValue();
			spines.add(idref);
		}

		nl = opf.getDocumentElement().getElementsByTagName("item");
		String ncx_file = null;
		Map<String, String> items = new HashMap<String, String>();
		for (int i = 0; i < nl.getLength(); i++) {
			NamedNodeMap attributes = nl.item(i).getAttributes();
			String id = attributes.getNamedItem("id").getNodeValue();
			if (id.equals("ncx")) {
				ncx_file = ops_path + attributes.getNamedItem("href").getNodeValue();
				continue;
			}
			items.put(id, attributes.getNamedItem("href").getNodeValue());
		}

		List<String> hrefs = new ArrayList<String>();
		for (String spine : spines)
			hrefs.add(items.get(spine));

		if (ncx_file == null)
			throw new Exception("Error parser the ops file:\"" + file.getPath() + "\"");

		state = RS.none;
		pi = -65535;
		nps = new ArrayList<TOCRecord>();
		ps.clear();

		is = zf.getInputStream(zf.getEntry(ncx_file));
		parse(new InputSource(is));

		int i = 1;
		NavPoint cur = (NavPoint) nps.get(0);
		NavPoint np = nps.size() > 0 ? (NavPoint) nps.get(i) : null;
		for (String href : hrefs)
			if (np != null && href.equals(np.href.get(0))) {
				i++;
				cur = np;
				np = i < nps.size() ? (NavPoint) nps.get(i) : null;
			} else
				cur.href.add(href);

		if (nps.isEmpty())
			throw new Exception("Error parser the ncx file:\"" + file.getPath() + "\"");
		if (ri.chapter >= nps.size())
			throw new Exception(
				String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

		return new EPubBook(zf, ri, nps, ops_path);
	}
}
