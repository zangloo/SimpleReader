package net.lzrj.SimpleReader;

import java.io.IOException;
import java.util.List;

public interface HtmlContentNodeCallback
{
	ContentLine createImage(List<ContentLine> lines, String src);

	String getCss(String href) throws IOException;
}
