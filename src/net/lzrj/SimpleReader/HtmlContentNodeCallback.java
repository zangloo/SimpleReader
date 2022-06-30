package net.lzrj.SimpleReader;

import java.io.IOException;

public interface HtmlContentNodeCallback
{
	UString.ImageValue imageValue(String src);

	String getCss(String href) throws IOException;
}
