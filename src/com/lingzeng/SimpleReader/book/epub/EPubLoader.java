package com.lingzeng.SimpleReader.book.epub;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import com.lingzeng.SimpleReader.Config;
import com.lingzeng.SimpleReader.book.BookLoader;
import com.lingzeng.SimpleReader.book.TOCRecord;
import com.lingzeng.SimpleReader.vfs.RealFile;
import com.lingzeng.SimpleReader.vfs.VFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-27
 * Time: 上午9:07
 */
public class EPubLoader implements BookLoader.Loader
{
	private static final String suffix = "epub";
	private static final String meta_file = "META-INF/container.xml";

	public EPubLoader()
	{
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
	}

	public boolean isBelong(VFile f)
	{
		return f.getPath().toLowerCase().endsWith("." + suffix);
	}

	public com.lingzeng.SimpleReader.book.Book load(VFile file, Config.ReadingInfo ri) throws Exception
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

		is = zf.getInputStream(zf.getEntry(ncx_file));
		EPubReader reader = new EPubReader();
		ArrayList<TOCRecord> nps = reader.read(is);

		int i = 1;
		NavPoint cur = null;
		NavPoint np = (NavPoint) nps.get(0);
		for (String href : hrefs)
			if (np != null && href.equals(np.href.get(0))) {
				i++;
				cur = np;
				np = i < nps.size() ? (NavPoint) nps.get(i) : null;
			} else if (cur == null) {
				cur = new NavPoint(0, 0);
				cur.href.add(href);
				nps.add(0, cur);
			} else
				cur.href.add(href);

		if (nps.isEmpty())
			throw new Exception("Error parser the ncx file:\"" + file.getPath() + "\"");
		if (ri.chapter >= nps.size())
			throw new Exception(
				String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

		return new EPubBook(zf, ri, nps, ops_path);
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
}
