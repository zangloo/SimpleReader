package net.lzrj.SimpleReader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import cz.vutbr.web.css.*;
import cz.vutbr.web.csskit.TermFactoryImpl;
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
	public static final String IMAGE_CHAR = "🖼";
	public static final int detectFileReadBlockSize = 2048;
	public static final byte[] detectFileReadBuffer = new byte[detectFileReadBlockSize];
	public static final String CONTENT_COLOR_FOR_STYLE_KEY = "data-simple-reader-color";
	public static final String CONTENT_BACKGROUND_FOR_STYLE_KEY = "data-simple-reader-background";
	public static final String CONTENT_FONT_SIZE_FOR_STYLE_KEY = "data-simple-reader-font-size";
	public static final String CONTENT_TEXT_BORDER_FOR_STYLE_KEY = "data-simple-reader-text-border";
	public static final String CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY = "data-simple-reader-text-underline";

	private static final String[] NEWLINE_CLASSES = new String[]{"contents", "toc", "mulu"};

	public static class HtmlContent
	{
		public final List<UString> lines;
		public final LinkedHashMap<String, Content.Position> fragmentMap;

		public HtmlContent(List<UString> lines, LinkedHashMap<String, Content.Position> fragmentMap)
		{
			this.lines = lines;
			this.fragmentMap = fragmentMap;
		}
	}

	private static class ParseContext
	{
		final List<UString> lines = new ArrayList<>();
		final LinkedHashMap<String, Content.Position> fragmentMap = new LinkedHashMap<>();
		UString buf = new UString("");
		HtmlContentNodeCallback nodeCallback;

		private ParseContext(HtmlContentNodeCallback nodeCallback)
		{
			this.nodeCallback = nodeCallback;
		}

		HashMap<TextStyleType, Object> styleText(Element element, HashMap<TextStyleType, Object> styles)
		{
			HashMap<TextStyleType, Object> newStyles = new HashMap<>();
			if (element.hasAttr(CONTENT_TEXT_BORDER_FOR_STYLE_KEY))
				newStyles.put(TextStyleType.border, true);
			if (element.hasAttr(CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY))
				newStyles.put(TextStyleType.border, true);
			String color = element.attr(CONTENT_COLOR_FOR_STYLE_KEY);
			if (color.length() > 0)
				try {
					newStyles.put(TextStyleType.color, Integer.parseInt(color));
				} catch (NumberFormatException ignore) {
				}
			String background = element.attr(CONTENT_BACKGROUND_FOR_STYLE_KEY);
			if (background.length() > 0)
				try {
					newStyles.put(TextStyleType.background, Integer.parseInt(background));
				} catch (NumberFormatException ignore) {
				}
			String fontSize = element.attr(CONTENT_FONT_SIZE_FOR_STYLE_KEY);
			if (fontSize.length() > 0)
				try {
					newStyles.put(TextStyleType.fontSize, Integer.parseInt(fontSize));
				} catch (NumberFormatException ignore) {
				}
			return newStyles;
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
									HashMap<TextStyleType, Object> styles = new HashMap<>();
									for (Declaration declaration : (RuleSet) rule) {
										String property = declaration.getProperty();
										if ("font-size".equals(property)) {
											Term<?> term = declaration.get(0);
											if (term instanceof TermPercent)
												styles.put(TextStyleType.fontSize, ((TermPercent) term).getValue().intValue());
											else if (term instanceof TermLength) {
												TermLength length = (TermLength) term;
												if (TermNumeric.Unit.em.equals(length.getUnit()))
													styles.put(TextStyleType.fontSize, (int) (length.getValue() * 100));
											} else if (term instanceof TermIdent)
												switch (((TermIdent) term).getValue().toLowerCase()) {
													case "smaller":
														styles.put(TextStyleType.fontSize, fontLevelToPercent(2));
														break;
													case "larger":
														styles.put(TextStyleType.fontSize, fontLevelToPercent(4));
														break;
												}
										}
										if ("border-width".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermLength && ((TermLength) term).getValue() > 0)
													styles.put(TextStyleType.border, true);
										if ("border".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermLength && ((TermLength) term).getValue() > 0)
													styles.put(TextStyleType.border, true);
										if ("text-decoration-line".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermIdent && "underline".equals(((TermIdent) term).getValue()))
													styles.put(TextStyleType.underline, true);
										if ("color".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermColor) {
													Integer color = parseColor((TermColor) term);
													if (color != null)
														styles.put(TextStyleType.color, color);
												}
										if ("background-color".equals(property))
											for (Term<?> term : declaration)
												if (term instanceof TermColor) {
													switch (((TermColor) term).getKeyword()) {
														case none:
															cz.vutbr.web.csskit.Color cssColor = ((TermColor) term).getValue();
															styles.put(TextStyleType.background, cssColor.getRGB());
															break;
														case TRANSPARENT:
															styles.put(TextStyleType.background, android.graphics.Color.TRANSPARENT);
															break;
														case CURRENT_COLOR:
															break;
													}
												}
									}
									if (!styles.isEmpty())
										for (CombinedSelector selector : ((RuleSet) rule).getSelectors())
											try {
												for (Element e : document.select(selector.toString())) {
													for (Map.Entry<TextStyleType, Object> entry : styles.entrySet())
														switch (entry.getKey()) {
															case underline:
																e.attr(CONTENT_TEXT_UNDERLINE_FOR_STYLE_KEY, true);
																break;
															case border:
																e.attr(CONTENT_TEXT_BORDER_FOR_STYLE_KEY, true);
																break;
															case fontSize:
																e.attr(CONTENT_FONT_SIZE_FOR_STYLE_KEY, entry.getValue().toString());
																break;
															case color:
																e.attr(CONTENT_COLOR_FOR_STYLE_KEY, entry.getValue().toString());
																break;
															case background:
																e.attr(CONTENT_BACKGROUND_FOR_STYLE_KEY, entry.getValue().toString());
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
		HTML2Text(document.body(), context, new HashMap<TextStyleType, Object>());
		if (context.buf.length() > 0)
			context.lines.add(context.buf);
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

	private static void HTML2Text(Element node, ParseContext context, HashMap<TextStyleType, Object> styles)
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
						context.buf.concat(" ", styles);
					context.buf.concat(text, styles);
				}
			} else if (child instanceof Element) {
				final Element element = (Element) child;
				String tagName = element.tagName();
				Set<String> classes = element.classNames();
				HashMap<TextStyleType, Object> childStyles = context.styleText(element, styles);
				int line = context.lines.size();
				int offset = context.buf.length();
				switch (tagName.toLowerCase()) {
					case "div":
					case "dt":
						newlineForClass(classes, context);
						HTML2Text(element, context, childStyles);
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
						HTML2Text(element, context, childStyles);
						if (context.buf.length() > 0) {
							context.buf.paragraph();
							pushBuf(context);
						}
						break;
					case "br":
						if (context.buf.length() > 0)
							pushBuf(context);
						HTML2Text(element, context, childStyles);
						break;
					case "img":
						if (context.nodeCallback != null) {
							UString.ImageValue image = context.nodeCallback.imageValue(element.attr("src"));
							if (image != null) {
								childStyles.clear();
								childStyles.put(TextStyleType.image, image);
								context.buf.concat(IMAGE_CHAR, childStyles);
							}
						}
						break;
					case "image":
						if (context.nodeCallback != null) {
							UString.ImageValue image = context.nodeCallback.imageValue(element.attr("xlink:href"));
							if (image != null) {
								childStyles.clear();
								childStyles.put(TextStyleType.image, image);
								context.buf.concat(IMAGE_CHAR, childStyles);
							}
						}
						break;
					case "font":
						String childFontLevelText = element.attr("size");
						if (childFontLevelText.length() > 0) try {
							int childFontLLevel = Integer.parseInt(childFontLevelText);
							int fontSize = fontLevelToPercent(childFontLLevel);
							childStyles.put(TextStyleType.fontSize, fontSize);
						} catch (NumberFormatException ignore) {
						}
						String childFontColor = element.attr("color");
						if (childFontColor.length() > 0) {
							TermColor term = TermFactoryImpl.getInstance().createColor(childFontColor);
							Integer color = parseColor(term);
							if (color != null)
								childStyles.put(TextStyleType.color, color);
						}
						HTML2Text(element, context, childStyles);
						break;
					case "a":
						String href = element.attr("href");
						if (href.length() > 1)
							childStyles.put(TextStyleType.link, href);
						HTML2Text(element, context, childStyles);
						break;
					default:
						HTML2Text(element, context, childStyles);
						break;
				}
				if (!styles.isEmpty()) {
					int lines = context.lines.size();
					int to = context.buf.length();
					if (lines > line) {
						for (int i = line; i < lines; i++) {
							UString text = context.lines.get(i);
							addCurrentStyles(text, 0, text.length(), styles);
						}
						if (to > 0)
							addCurrentStyles(context.buf, 0, to, styles);
					} else if (to > offset)
						addCurrentStyles(context.buf, offset, to, styles);
				}
			}
	}

	private static void addCurrentStyles(UString text, int from, int to, HashMap<TextStyleType, Object> styles)
	{
		Object value = styles.get(TextStyleType.border);
		if (value != null)
			text.addStyle(from, to, TextStyleType.border, true);
		value = styles.get(TextStyleType.link);
		if (value != null)
			text.addStyle(from, to, TextStyleType.link, value);
		else {
			value = styles.get(TextStyleType.underline);
			if (value != null)
				text.addStyle(from, to, TextStyleType.underline, true);
		}
		value = styles.get(TextStyleType.fontSize);
		if (value != null)
			text.addStyle(from, to, TextStyleType.fontSize, value);
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

	private static int fontLevelToPercent(int level)
	{
		switch (level) {
			case 1:
				return 3 * 100 / 5;
			case 2:
				return 8 * 100 / 9;
			case 3:
			default:
				return 100;
			case 4:
				return 6 * 100 / 5;
			case 5:
				return 3 * 100 / 2;
			case 6:
				return 2 * 100;
			case 7:
				return 3 * 100;
		}
	}

	private static Integer parseColor(TermColor color)
	{
		switch (color.getKeyword()) {
			case none:
				cz.vutbr.web.csskit.Color cssColor = color.getValue();
				return cssColor.getRGB();
			case TRANSPARENT:
				return android.graphics.Color.TRANSPARENT;
			case CURRENT_COLOR:
			default:
				return null;
		}
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
