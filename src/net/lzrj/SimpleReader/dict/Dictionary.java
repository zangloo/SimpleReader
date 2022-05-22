package net.lzrj.SimpleReader.dict;

public interface Dictionary extends DictionaryInfo
{
	boolean exists(String word);

	String query(String word);

	int maxWordLength();

	boolean webView();
}
