package com.lingzeng.SimpleReader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.lingzeng.SimpleReader.ContentImageLoader;
import com.lingzeng.SimpleReader.ContentLine;
import com.lingzeng.SimpleReader.UString;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-9-28
 * Time: 上午11:47
 */
public class BookUtil
{
	public static final String defaultCNEncode = "GBK";
	public static final String cnEncodePrefix = "GB";

	public static final int detectFileReadBlockSize = 2048;
	public static byte[] detectFileReadBuffer = new byte[detectFileReadBlockSize];

	// put all text into lines
	public static void HTML2Text(Element node, List<ContentLine> lines)
	{
		HTML2Text(node, lines, null);
	}

	// if images != null, this function will return with all images href.
	public static void HTML2Text(Element node, List<ContentLine> lines, @Nullable ContentImageLoader imageLoader)
	{
		for (Node child : node.childNodes()) {
			if (child instanceof TextNode) {
				String t = ((TextNode) child).text();
				if (t.trim().length() > 0)
					lines.add(new UString(t));
			} else if (child instanceof Element) {
				final Element e = (Element) child;
				if (imageLoader != null && e.tagName().equalsIgnoreCase("img"))
					lines.add(imageLoader.loadImage(e.attr("src")));
				else
					HTML2Text(e, lines, imageLoader);
			}
		}
	}

	public static String detect(InputStream is)
	{
		UniversalDetector detector = new UniversalDetector(null);

		int len;
		try {
			while ((len = is.read(detectFileReadBuffer)) != -1) {
				detector.handleData(detectFileReadBuffer, 0, len);
				if (detector.isDone())
					break;
			}
		} catch (IOException e) {
			return defaultCNEncode;
		}

		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		detector.reset();

		if (encoding == null)
			return defaultCNEncode;
		if (encoding.indexOf(cnEncodePrefix) == 0)
			encoding = defaultCNEncode;

		return encoding;
	}

	static public String concatPath(String prefix, String path)
	{
		if (prefix.endsWith("/"))
			prefix = prefix.substring(0, prefix.length() - 1);
		while (path.startsWith("../")) {
			path = path.substring(3);
			int index = prefix.indexOf('/');
			if (index == -1)
				prefix = "";
			else
				prefix = prefix.substring(0, index);
		}
		return prefix + "/" + path;
	}

	static public Bitmap loadPicFromZip(ZipFile zip, String picName)
	{
		Bitmap bm = null;
		try {
			ZipArchiveEntry zae;
			if (picName == null)
				zae = (ZipArchiveEntry) zip.getEntries().nextElement();
			else
				zae = zip.getEntry(picName);
			if (zae == null)
				zae = (ZipArchiveEntry) zip.getEntries().nextElement();
			InputStream is = zip.getInputStream(zae);
			int size = (int) zae.getSize();
			if (size <= 0)
				return null;
			byte[] bs = new byte[size];
			int cnt = 0;
			while (cnt < size) {
				int s = is.read(bs, cnt, size - cnt);
				if (s == -1)
					break;
				cnt += s;
			}
			if (cnt != size)
				return null;
			bm = BitmapFactory.decodeByteArray(bs, 0, bs.length);
		} catch (IOException e) {
			Log.e("BookUtil.loadPicFromZip", e.getMessage());
		}
		return bm;
	}
}
