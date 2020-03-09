package com.lingzeng.SimpleReader.book.epub;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.XMLReaderFactory;
import com.lingzeng.SimpleReader.book.TOCRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: zang.loo
 * Date: 2020/3/9
 * Time: 下午6:28
 */
public class EPubReader extends XMLReaderAdapter
{
	private static final String TAG_NAVMAP = "navmap";
	private static final String TAG_NAVPOINT = "navpoint";
	private static final String TAG_NAVLABEL = "navlabel";
	private static final String TAG_CONTENT = "content";
	private static final String TAG_TEXT = "text";
	private static final String ATTRIBUTE_PLAYORDER = "playOrder";
	private final Stack<EPubLoader.NavPoint> ps = new Stack<EPubLoader.NavPoint>();
	private RS state = RS.none;
	private ArrayList<TOCRecord> nps = new ArrayList<TOCRecord>();
	private int pi = -65535;

	public EPubReader() throws SAXException
	{
		super(XMLReaderFactory.createXMLReader());
	}

	public ArrayList<TOCRecord> read(InputStream is) throws IOException, SAXException
	{
		parse(new InputSource(is));
		return nps;
	}

	@Override
	public void characters(char[] ch, int start, int length)
	{
		if (state == RS.text)
			ps.lastElement().title += new String(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts)
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
	public void endElement(String uri, String localName, String qName)
	{
		String tag = qName.toLowerCase().intern();
		switch (state) {
			case map:
				if (TAG_NAVMAP.equals(tag))
					state = RS.none;
				break;
			case point:
				if (TAG_NAVPOINT.equals(tag)) {
					final EPubLoader.NavPoint np = ps.pop();
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
	public void endDocument()
	{
		Collections.sort(nps, new Comparator<TOCRecord>()
		{
			public int compare(TOCRecord o1, TOCRecord o2)
			{
				return ((EPubLoader.NavPoint) o1).order - ((EPubLoader.NavPoint) o2).order;
			}
		});
	}

	private void pushNP(final String o)
	{
		final int i = (o != null) ? Integer.parseInt(o) : pi++;
		ps.push(new EPubLoader.NavPoint(i, ps.size()));
	}

	// reading state enum
	private enum RS
	{
		none, map, point, label, text
	}
}
