package com.lingzeng.SimpleReader;

import org.jsoup.nodes.Element;

import java.util.List;

public interface HtmlContentNodeCallback
{
	void process(Element element);

	void addText(List<ContentLine> lines, UString text);

	void addImage(List<ContentLine> lines, String src);
}
