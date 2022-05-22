package net.lzrj.SimpleReader.dict;

import java.io.IOException;
import java.util.zip.DataFormatException;

public interface DictionaryInfo
{
	String name();

	String path();

	Dictionary load() throws IOException, DataFormatException;
}
