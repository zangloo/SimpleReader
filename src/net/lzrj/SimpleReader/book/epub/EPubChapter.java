package net.lzrj.SimpleReader.book.epub;

import net.lzrj.SimpleReader.book.Content;
import net.lzrj.SimpleReader.book.ContentBase;

import java.util.LinkedHashMap;

public class EPubChapter
{
	final String path;
	final ContentBase content;
	final LinkedHashMap<String, Content.Position> fragmentMap;

	EPubChapter(String path, ContentBase content, LinkedHashMap<String, Content.Position> fragmentMap)
	{
		this.path = path;
		this.content = content;
		this.fragmentMap = fragmentMap;
	}
}
