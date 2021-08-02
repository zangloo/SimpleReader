package net.lzrj.SimpleReader;

public interface ContentLine
{
	ContentLineType type();

	int length();

	boolean isImage();
}
