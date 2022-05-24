package net.lzrj.SimpleReader;

import java.util.List;

public interface HtmlContentNodeCallback
{
	ContentLine createImage(List<ContentLine> lines, String src);
}
