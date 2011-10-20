package zhang.lu.SimpleReader.Book;

import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.VFS.CloudFile;
import zhang.lu.SimpleReader.VFS.VFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-10-16
 * Time: 下午3:14
 */
public class SRBOnline extends PlainTextContent implements BookLoader.Loader
{
	public static class OnlineChapterInfo extends ChapterInfo
	{
		private ArrayList<String> lines = null;
		private HashMap<Long, String> notes = null;

		public OnlineChapterInfo(String t)
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

	private int chapter;
	private ArrayList<ChapterInfo> chapters = new ArrayList<ChapterInfo>();
	private CloudFile cf = null;
	private CloudFile.OnlineProperty op = null;

	public boolean isBelong(VFile f)
	{
		return (CloudFile.class.isInstance(f)) &&
			(f.getPath().toLowerCase().endsWith("." + SimpleReaderBook.suffix));
	}

	public BookContent load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		if (!isBelong(file))
			throw new IOException("Not supported");

		cf = (CloudFile) file;
		chapters = cf.getChapters();
		op = cf.getProperty();
		if (op == null)
			throw new IOException("Can't open file");
		chapter = ri.chapter;
		loadChapter(chapter);
		return this;
	}

	public void unload(BookContent aBook)
	{
		cf = null;
		op = null;
		chapters.clear();
	}

	@Override
	public int getChapterCount()
	{
		return chapters.size();
	}

	@Override
	public String getChapterTitle(int index)
	{
		return chapters.get(index).title;
	}

	@Override
	public ArrayList<ChapterInfo> getChapterInfoList()
	{
		return chapters;
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
			OnlineChapterInfo oci = (OnlineChapterInfo) chapters.get(index);
			if (oci.lines == null) {
				oci.lines = cf.getLines(chapter);
				if (op.hasNotes)
					oci.notes = cf.getNotes(chapter);
			}
			setContent(oci.lines);
		} catch (Exception e) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getMessage());
			setContent(list);
		}
		return true;
	}

	@Override
	public boolean hasNotes()
	{
		return op.hasNotes;
	}

	@Override
	public String getNote(int line, int offset)
	{
		OnlineChapterInfo oci = (OnlineChapterInfo) chapters.get(getCurrChapter());
		if (!op.hasNotes)
			return null;
		if (line >= oci.lines.size())
			return null;
		String l = oci.lines.get(line);
		if (offset >= l.length())
			return null;
		if (l.charAt(offset) != op.mark)
			return null;
		return oci.getNote(line + op.indexBase, offset);
	}
}
