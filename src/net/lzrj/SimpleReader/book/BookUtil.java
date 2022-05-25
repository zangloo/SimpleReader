package net.lzrj.SimpleReader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import cz.vutbr.web.css.*;
import net.lzrj.SimpleReader.ContentLine;
import net.lzrj.SimpleReader.HtmlContentNodeCallback;
import net.lzrj.SimpleReader.UString;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Selector;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
	public static final byte[] detectFileReadBuffer = new byte[detectFileReadBlockSize];
	public static final int DEFAULT_CONTENT_FONT_LEVEL = 3;
	public static final int CONTENT_FONT_SIZE_DELTA_STEP = 20;
	public static final String CONTENT_FONT_SIZE_FOR_STYLE_KEY = "data-simple-reader-font-size";
	public static final String CONTENT_TEXT_BORDER_FOR_STYLE_KEY = "data-simple-reader-text-border";
	public static final String CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY = "data-simple-reader-text-underline";

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

	private static class ParseContext
	{
		final List<ContentLine> lines = new ArrayList<>();
		final LinkedHashMap<String, Content.Position> fragmentMap = new LinkedHashMap<>();
		UString buf = new UString("");
		HtmlContentNodeCallback nodeCallback;

		private ParseContext(HtmlContentNodeCallback nodeCallback)
		{
			this.nodeCallback = nodeCallback;
		}

		Integer styleFontSize(Element element, int parentFontSize)
		{
			String fontSize = element.attr(CONTENT_FONT_SIZE_FOR_STYLE_KEY);
			if (fontSize.length() > 0)
				try {
					return Integer.parseInt(fontSize);
				} catch (NumberFormatException ignore) {
				}
			return parentFontSize;
		}

		TextStyleType styleText(Element element, TextStyleType parentTextStyleType)
		{
			if (element.hasAttr(CONTENT_TEXT_BORDER_FOR_STYLE_KEY))
				return TextStyleType.border;
			if (element.hasAttr(CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY))
				return TextStyleType.underline;
			return parentTextStyleType;
		}
	}

	public static HtmlContent HTML2Text(Document document)
	{
		return HTML2Text(document, null);
	}

	public static HtmlContent HTML2Text(Document document, HtmlContentNodeCallback nodeCallback)
	{
		if (nodeCallback != null)
			for (Element element : document.select("link")) {
				String href = element.attr("href");
				if (href.toLowerCase().endsWith(".css"))
					try {
						String css = nodeCallback.getCss(href);
						if (css != null) {
							StyleSheet sheet = CSSFactory.parseString(css, null, new NetworkProcessor()
							{   // no importing from network, no networking permission needed, for now
								@Override
								public InputStream fetch(URL url)
								{
									return new ByteArrayInputStream(new byte[0]);
								}
							});
							for (RuleBlock<?> rule : sheet)
								if (rule instanceof RuleSet) {
									Integer fontSize = null;
									TextStyleType textStyleType = null;
									for (Declaration declaration : (RuleSet) rule) {
										String property = declaration.getProperty();
										if ("font-size".equals(property)) {
											Term<?> term = declaration.get(0);
											if (term instanceof TermPercent)
												fontSize = ((TermPercent) term).getValue().intValue();
											else if (term instanceof TermLength) {
												TermLength length = (TermLength) term;
												if (TermNumeric.Unit.em.equals(length.getUnit()))
													fontSize = (int) (length.getValue() * 100);
											}
										}
										if ("border-width".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermLength && ((TermLength) term).getValue() > 0)
													textStyleType = TextStyleType.border;
										if ("border".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermLength && ((TermLength) term).getValue() > 0)
													textStyleType = TextStyleType.border;
										if ("text-decoration-line".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermIdent && "underline".equals(((TermIdent) term).getValue()))
													textStyleType = TextStyleType.underline;
									}
									if (fontSize != null || textStyleType != null)
										for (CombinedSelector selector : ((RuleSet) rule).getSelectors())
											try {
												for (Element e : document.select(selector.toString())) {
													if (fontSize != null)
														e.attr(CONTENT_FONT_SIZE_FOR_STYLE_KEY, Integer.toString(fontSize));
													if (textStyleType != null)
														switch (textStyleType) {
															case border:
																e.attr(CONTENT_TEXT_BORDER_FOR_STYLE_KEY, true);
																break;
															case underline:
																e.attr(CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY, true);
																break;
														}
												}
											} catch (Selector.SelectorParseException ignore) {
											}
								}
						}
					} catch (IOException | CSSException e) {
						e.printStackTrace();
					}
			}

		ParseContext context = new ParseContext(nodeCallback);
		HTML2Text(document.body(), 100, context, null);
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

	private static void HTML2Text(Element node, int fontSize, ParseContext context, TextStyleType textStyleType)
	{
		String elementId = node.id();
		if (elementId.length() > 0)
			context.fragmentMap.put(elementId, new Content.Position(context.lines.size(), context.buf.length()));
		for (Node child : node.childNodes())
			if (child instanceof TextNode) {
				String text = strip(((TextNode) child).text());
				if (text.length() > 0) {
					if (context.buf.length() > 0
						&& !Character.isLetterOrDigit(context.buf.charAt(context.buf.length() - 1))
						&& !Character.isLetterOrDigit(text.charAt(0)))
						context.buf.concat(" ", textStyleType, fontSize);
					context.buf.concat(text, textStyleType, fontSize);
				}
			} else if (child instanceof Element) {
				final Element element = (Element) child;
				String tagName = element.tagName();
				Set<String> classes = element.classNames();
				int childFontSize = context.styleFontSize(element, fontSize);
				TextStyleType childTextStyleType = context.styleText(element, textStyleType);
				switch (tagName.toLowerCase()) {
					case "div":
					case "dt":
						newlineForClass(classes, context);
						HTML2Text(element, childFontSize, context, childTextStyleType);
						newlineForClass(classes, context);
						break;
					case "blockquote":
					case "p":
					case "h1":
					case "h2":
					case "h3":
					case "h4":
					case "h5":
					case "li":
						if (context.buf.length() > 0)
							pushBuf(context);
						context.buf.paragraph();
						HTML2Text(element, childFontSize, context, childTextStyleType);
						if (context.buf.length() > 0) {
							context.buf.paragraph();
							pushBuf(context);
						}
						break;
					case "br":
						if (context.buf.length() > 0)
							pushBuf(context);
						HTML2Text(element, childFontSize, context, childTextStyleType);
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
						String childFontLevelText = element.attr("size");
						if (childFontLevelText == null)
							HTML2Text(element, childFontSize, context, childTextStyleType);
						else try {
							int childFontLLevel = Integer.parseInt(childFontLevelText);
							childFontSize = 100 + (childFontLLevel - DEFAULT_CONTENT_FONT_LEVEL) * CONTENT_FONT_SIZE_DELTA_STEP;
							HTML2Text(element, childFontSize, context, childTextStyleType);
						} catch (NumberFormatException ignore) {
							HTML2Text(element, childFontSize, context, childTextStyleType);
						}
						break;
					case "a":
						HTML2Text(element, childFontSize, context, TextStyleType.underline);
						break;
					default:
						HTML2Text(element, childFontSize, context, childTextStyleType);
						break;
				}
			}
	}

	public static String detectCharset(InputStream is)
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
		if (prefix.length() == 0)
			return path;
		else
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
