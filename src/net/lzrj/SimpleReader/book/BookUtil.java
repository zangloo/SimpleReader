package net.lzrj.SimpleReader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.HtmlContentNodeCallback;
import net.lzrj.SimpleReader.UString;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
	public static int DEFAULT_CONTENT_FONT_SIZE = 3;

	private static final String[] NEWLINE_CLASSES = new String[]{"contents", "toc", "mulu"};

	public static class HtmlContent
	{
		public final List<ContentLine> lines;
		public final LinkedHashMap<String, Content.Position> fragmentMap;

		public HtmlContent(List<ContentLine> lines, LinkedHashMap<String, Content.Position> fragmentMap)
		{
			this.lines = lines;
			this.fragmentMap = fragmentMap;
		}
	}

	public static class ParseContext
	{
		final List<ContentLine> lines = new ArrayList<>();
		final LinkedHashMap<String, Content.Position> fragmentMap = new LinkedHashMap<>();
		UString buf = new UString("");
		HtmlContentNodeCallback nodeCallback;

		public ParseContext(HtmlContentNodeCallback nodeCallback)
		{
			this.nodeCallback = nodeCallback;
		}
	}

	// put all text into lines
	public static HtmlContent HTML2Text(Element node)
	{
		return HTML2Text(node, null);
	}

	public static HtmlContent HTML2Text(Element node, HtmlContentNodeCallback nodeCallback)
	{
		ParseContext context = new ParseContext(nodeCallback);
		HTML2Text(node, DEFAULT_CONTENT_FONT_SIZE, context, false);
		return new HtmlContent(context.lines, context.fragmentMap);
	}

	private static String strip(String text)
	{
		int length = text.length();
		int start = 0;
		for (; start < length; start++)
			if (!Character.isWhitespace(text.charAt(start)))
				break;
		int end = length - 1;
		for (; end >= start; end--)
			if (!Character.isWhitespace(text.charAt(end)))
				break;
		end++;
		if (start == 0 && end == length)
			return text;
		if (start >= end)
			return "";
		return text.substring(start, end);
	}

	private static void pushBuf(ParseContext context)
	{
		// ignore empty line if prev line is empty too.
		if (context.buf.length() == 0) {
			int lineCount = context.lines.size();
			if (lineCount == 0 || context.lines.get(lineCount - 1).length() == 0)
				return;
		}
		context.lines.add(context.buf);
		context.buf = new UString("");
	}

	private static void newlineForClass(Set<String> classes, ParseContext context)
	{
		if (context.buf.length() > 0)
			for (String newlineClass : NEWLINE_CLASSES)
				if (classes.contains(newlineClass)) {
					pushBuf(context);
					return;
				}
	}

	// if images != null, this function will return with all images href.
	private static void HTML2Text(Element node, int fontSize, ParseContext context, boolean underline)
	{
		for (Node child : node.childNodes())
			if (child instanceof TextNode) {
				String text = strip(((TextNode) child).text());
				if (text.length() > 0) {
					if (context.buf.length() > 0)
						context.buf.concat(" ", false, fontSize - DEFAULT_CONTENT_FONT_SIZE);
					context.buf.concat(text, underline, fontSize - DEFAULT_CONTENT_FONT_SIZE);
				}
			} else if (child instanceof Element) {
				final Element element = (Element) child;
				String elementId = element.id();
				if (elementId.length() > 0)
					context.fragmentMap.put(elementId, new Content.Position(context.lines.size(), context.buf.length()));
				String tagName = element.tagName();
				Set<String> classes = element.classNames();
				if (classes.contains("kindle-cn-underline"))
					underline = true;
				switch (tagName.toLowerCase()) {
					case "div":
					case "dt":
						newlineForClass(classes, context);
						HTML2Text(element, fontSize, context, underline);
						newlineForClass(classes, context);
						break;
					case "blockquote":
					case "p":
					case "h1":
					case "h2":
					case "h3":
					case "h4":
					case "li":
						if (context.buf.length() > 0)
							pushBuf(context);
						context.buf.paragraph();
						HTML2Text(element, fontSize, context, underline);
						if (context.buf.length() > 0) {
							context.buf.paragraph();
							pushBuf(context);
						}
						break;
					case "br":
						if (context.buf.length() > 0)
							pushBuf(context);
						HTML2Text(element, fontSize, context, underline);
						break;
					case "img":
						if (context.nodeCallback != null)
							context.lines.add(context.nodeCallback.createImage(context.lines, element.attr("src")));
						break;
					case "image":
						if (context.nodeCallback != null)
							context.lines.add(context.nodeCallback.createImage(context.lines, element.attr("xlink:href")));
						break;
					case "font":
						String childFontSizeText = element.attr("size");
						if (childFontSizeText == null)
							HTML2Text(element, fontSize, context, underline);
						else try {
							int childFontSize = Integer.parseInt(childFontSizeText);
							HTML2Text(element, childFontSize, context, underline);
						} catch (NumberFormatException ignore) {
							HTML2Text(element, fontSize, context, underline);
						}
						break;
					case "a":
						HTML2Text(element, fontSize, context, true);
						break;
					default:
						HTML2Text(element, fontSize, context, underline);
						break;
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
