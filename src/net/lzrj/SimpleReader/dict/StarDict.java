package net.lzrj.SimpleReader.dict;

import com.davidthomasbernal.stardict.parsers.IfoParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;

public class StarDict implements Dictionary
{
	static final String EXTENSION = ".ifo";
	private com.davidthomasbernal.stardict.dictionary.DictionaryInfo dictionaryInfo;
	private String filepath;
	private com.davidthomasbernal.stardict.Dictionary dictionary = null;
	private int maxWordLength = 1;

	public static DictionaryInfo info(File file) throws IOException
	{
		FileReader reader = new FileReader(file);
		IfoParser parser = new IfoParser();
		com.davidthomasbernal.stardict.dictionary.DictionaryInfo info = parser.parse(reader);
		StarDict dict = new StarDict();
		dict.dictionaryInfo = info;
		dict.filepath = file.getAbsolutePath();
		return dict;
	}

	public String name()
	{
		return dictionaryInfo.getName();
	}

	@Override
	public String path()
	{
		return filepath;
	}

	@Override
	public Dictionary load() throws IOException, DataFormatException
	{
		dictionary = com.davidthomasbernal.stardict.Dictionary.fromIfo(filepath, false);
		maxWordLength = 1;
		for (String word : dictionary.getWords())
			if (word.length() > maxWordLength)
				maxWordLength = word.length();
		return this;
	}

	@Override
	public boolean exists(String word)
	{
		return dictionary.containsWord(word);
	}

	@Override
	public String query(String word)
	{
		try {
			List<String> res = dictionary.getDefinitions(word);
			if (res.size() > 0)
				return res.get(0).replace("\n", "<br/>");
		} catch (DataFormatException | IOException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@Override
	public int maxWordLength()
	{
		return maxWordLength;
	}

	@Override
	public boolean webView()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return name();
	}
}
