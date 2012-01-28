package zhang.lu.SimpleReader.book.SRBOnline;

import org.jetbrains.annotations.Nullable;
import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.VFS.CloudFile;
import zhang.lu.SimpleReader.VFS.VFile;
import zhang.lu.SimpleReader.book.*;
import zhang.lu.SimpleReader.book.SimpleReader.SimpleReaderLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-10-16
 * Time: 下午3:14
 */
public class SRBOnlineLoader implements BookLoader.Loader
{
	public static class OnlineTOC extends TOCRecord
	{
		private ArrayList<String> lines = null;
		private HashMap<Long, String> notes = null;

		public OnlineTOC(String t)
		{
			super(t);
		}

		public static long key(int line, int offset)
		{
			return (((long) line) << 32) | offset;
		}

		public static void addNote(HashMap<Long, String> nn, int line, int offset, String note)
		{
			nn.put(key(line, offset), note);
		}

		private String getNote(int line, int offset)
		{
			return notes.get(key(line, offset));
		}
	}

	private static class SRBOnlineContent extends PlainTextContent
	{
		CloudFile.OnlineProperty op;
		OnlineTOC oci = null;

		SRBOnlineContent(CloudFile.OnlineProperty property)
		{
			super();
			op = property;
		}

		public void setOCI(@Nullable OnlineTOC info)
		{
			oci = info;
		}

		@Override
		public boolean hasNotes()
		{
			return op.hasNotes;
		}

		@Override
		public String getNote(int line, int offset)
		{
			if (!op.hasNotes)
				return null;
			if (line >= getLineCount())
				return null;
			String l = line(line);
			if (offset >= l.length())
				return null;
			if (l.charAt(offset) != op.mark)
				return null;
			if (oci == null)
				return null;
			return oci.getNote(line + op.indexBase, offset);
		}
	}

	private static class SRBOnlineBook extends ChaptersBook
	{
		private CloudFile cf = null;
		private CloudFile.OnlineProperty op = null;
		private SRBOnlineContent content;

		private SRBOnlineBook(VFile file, Config.ReadingInfo ri) throws IOException, URISyntaxException
		{
			cf = (CloudFile) file;
			TOC = cf.getChapters();
			op = cf.getProperty();
			if (op == null)
				throw new IOException("Can't open file");
			chapter = ri.chapter;

			if (ri.chapter >= TOC.size())
				throw new IOException(String.format("Error open chapter %d @ \"%s\"", ri.chapter, file.getPath()));

			content = new SRBOnlineContent(op);
			loadChapter(chapter);
		}

		@Override
		public int getCurrChapter()
		{
			return chapter - op.indexBase;
		}

		@Override
		protected boolean loadChapter(int index)
		{
			try {
				chapter = index + op.indexBase;
				OnlineTOC oci = (OnlineTOC) TOC.get(index);
				if (oci.lines == null) {
					oci.lines = cf.getLines(chapter);
					if (op.hasNotes)
						oci.notes = cf.getNotes(chapter);
				}
				content.setOCI(oci);
				content.setContent(oci.lines);
			} catch (Exception e) {
				ArrayList<String> list = new ArrayList<String>();
				list.add(e.getMessage());
				content.setOCI(null);
				content.setContent(list);
			}
			return true;
		}

		@Override
		public Content getContent(int index)
		{
			return content;
		}

		@Override
		public void close()
		{
			cf = null;
			op = null;
			TOC.clear();
		}
	}

	public boolean isBelong(VFile f)
	{
		return (CloudFile.class.isInstance(f)) &&
			(f.getPath().toLowerCase().endsWith("." + SimpleReaderLoader.suffix));
	}

	public Book load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		if (!isBelong(file))
			throw new IOException("Not supported");

		return new SRBOnlineBook(file, ri);
	}
}
