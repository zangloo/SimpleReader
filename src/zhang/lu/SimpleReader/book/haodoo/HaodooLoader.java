package zhang.lu.SimpleReader.book.haodoo;

import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.UString;
import zhang.lu.SimpleReader.book.*;
import zhang.lu.SimpleReader.vfs.VFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 11-3-5
 * Time: 上午10:23
 */

/*
機子及作業系統越來越多，我不可能逐一撰寫閱讀軟體，因而特將uPDB及PDB檔詳細規格公布如下，方便有興趣、有時間、能寫程式的讀友，為新機種撰寫閱讀軟體。唯一的請求是：您撰寫閱讀軟體的目的不是圖利，而是造福讀友，讓讀友們可免費使用。謝謝。

    PDB是源自Palm作業系統的一個單一檔案，簡易資料庫。
    每一個PDB檔含N筆不定長度的資料(record)。
    PDB檔最前面當然要有個Header，定義本資料庫的特性。
    因資料長度非固定，無法計算位置。所以Header之後，是各筆資料所在的位置，可以用來讀資料及計算每筆資料的長度。
    之後，就是一筆一筆的資料，沒什麼大學問可言。

    檔案的前78個bytes，是Header[0..77]：
        Header[0..34]舊版是放書名，新版是放作者。可以不理。
        Header[35]是2，舊版是1。可以不理。
        Header[36..43]是為Palm而加的兩個日期，可以不理。
        Header[44..59]都是0。可以不理。
        Header[60..63]是"BOOK"。可以不理。
        Header[64..67]是判別的關鍵，PDB是"MTIT"，uPDB是"MTIU"。
        Header[68..75]都是0。可以不理。
        Header[76..77]是record數，N(章數)加2(目錄及書籤)。

    每筆資料的起始位置及屬性，依Palm的規格是8個bytes，前4個bytes是位置，後4個bytes是0。一共有 (N+2) * 8 bytes。

    第一筆資料定義書的屬性，是8個空白字元、書名、章數及目錄：
        (PDB檔)
        8個空白btyes，可以不理；
        之後接書名是Big5碼，後接三個ESC(即27)；
        之後接章數(ASCII string)，後接一個ESC；
        之後接目錄，各章之標題是以ESC分隔。
        (uPDB檔)
        8個空白btyes，可以不理；
        之後接書名是Unicode碼，後接三個ESC(即27,0)；
        之後接章數(ASCII string)，後接一個ESC (27, 0)；
        之後接目錄，各章之標題是以CR(13,0) NL(10,0) 分隔。

    再來是N筆資料，每筆是一章的內容，PDB檔是Big5碼(是null-terminated string，最後一個byte是0)，uPDB檔是Unicode碼。

    第N+2筆資料是書籤，預設是-1。可以不理。

 */
public class HaodooLoader extends ChaptersBook implements BookLoader.Loader
{
	private static final String[] suffixes = {"pdb", "updb"};

	public static final int HEADER_LENGTH = 78;
	public static final String PDB_ID = "MTIT";
	public static final String UPDB_ID = "MTIU";
	public static final String PALMDOC_ID = "REAd";
	public static final byte[] PDB_SEPARATOR = new byte[]{0x1b};
	public static final byte[] UPDB_TITLE_SEPARATOR = new byte[]{0x0d, 0x00, 0x0a, 0x00};
	public static final byte[] UPDB_ESCAPE_SEPARATOR = new byte[]{0x1b, 0x00};
	public static final int MAX_REC_SIZE = 4096;

	public static final int TEXT_COUNT_OFFSET = 8;
	public static final int RECODES_COUNT_OFFSET = 76;
	public static final int ID_OFFSET = 64;
	public static final int ID_LENGTH = 4;

	//"★★★★★★★以下內容★★︽本版︾★★無法顯示★★★★★★★";
	public static final byte[] ENCRYPT_MARK = {(byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA5, (byte) 0x48, (byte) 0xA4, (byte) 0x55, (byte) 0xA4, (byte) 0xBA, (byte) 0xAE, (byte) 0x65, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0x6F, (byte) 0xA5, (byte) 0xBB, (byte) 0xAA, (byte) 0xA9, (byte) 0xA1, (byte) 0x70, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xB5, (byte) 0x4C, (byte) 0xAA, (byte) 0x6B, (byte) 0xC5, (byte) 0xE3, (byte) 0xA5, (byte) 0xDC, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A};

	private enum BookType
	{
		palmDoc, pdb, updb
	}

	private static class HaodooTOCR extends TOCRecord
	{
		ArrayList<UString> lines;

		HaodooTOCR(String t)
		{
			super(t);
			lines = new ArrayList<UString>();
		}
	}

	private static int recordCount;
	private static Vector<Long> recordOffsets;
	private static String encode;
	private static boolean encrypted = false;
	private static BookType bookType;
	private static boolean compression;
	private static int txtCount;
	private static byte[] recBuf = new byte[MAX_REC_SIZE * 2];
	private static int recPos = 0;

	private PlainTextContent content = new PlainTextContent();

	protected static String PDBEncode = "BIG5";
	protected static String UPDBEncode = "UTF-16LE";

	private static void unEncrypt(byte[] rec, int offset)
	{
		for (int i = offset; i < rec.length; i++)
			// byte is signed, so this code ugly
			if (rec[i] < -1)
				rec[++i]--;
	}

	// return -1 for not found
	private static int findSeparator(byte[] rec, byte[] separator, int offset)
	{
		for (int i = offset; i < rec.length; i++)
			if (rec[i] == separator[0]) {
				int j;
				for (j = 1; j < separator.length; j++)
					if (rec[i + j] != separator[j])
						break;
				if (j == separator.length)
					return i;
			}
		return -1;
	}

	private void formatTitle(byte[] rec) throws UnsupportedEncodingException
	{
		byte[] escape = (bookType == BookType.pdb) ? PDB_SEPARATOR : UPDB_ESCAPE_SEPARATOR;
		int np = findSeparator(rec, escape, 8);
		int p = findSeparator(rec, escape, np + 3 * escape.length) + escape.length;

		byte[] separator = (bookType == BookType.pdb) ? PDB_SEPARATOR : UPDB_TITLE_SEPARATOR;
		while ((np = findSeparator(rec, separator, p)) >= 0) {
			//text.add(s.substring(p, np));
			String s = new String(rec, p, np - p, encode);
			TOC.add(new HaodooTOCR(s));
			p = np + separator.length;
		}

		if (p < rec.length)
			TOC.add(new HaodooTOCR(new String(rec, p, rec.length - p, encode)));

		txtCount = recordCount - 2;
	}

	private static void format(byte[] rec, HaodooTOCR ci)
	{
		String s;
		int offset = 0;
		int len = (bookType == BookType.pdb) ? rec.length - 1 : rec.length;
		try {
			// updb has no encrypted, check pdb only
			if (bookType == BookType.pdb) {
				if (!encrypted) {
					int i;
					for (i = 0; i < ENCRYPT_MARK.length; i++)
						if (ENCRYPT_MARK[i] != rec[i])
							break;
					if (i == ENCRYPT_MARK.length) {
						encrypted = true;
						offset = ENCRYPT_MARK.length;
						len = len - ENCRYPT_MARK.length;
					}
				}
				if (encrypted)
					unEncrypt(rec, offset);
			}
			s = new String(rec, offset, len, encode);
		} catch (UnsupportedEncodingException e) {
			return;
		}

		int p = 0, np;
		while ((np = s.indexOf("\r\n", p)) >= 0) {
			ci.lines.add(new UString(s.substring(p, np)));
			p = np + 2;
		}

		if (p < s.length()) {
			ci.lines.add(new UString(s.substring(p)));
		}
	}

	private static void readHeader(InputStream is) throws Exception
	{
		byte[] header = new byte[HEADER_LENGTH];

		if (is.read(header) != HEADER_LENGTH)
			throw new IOException("readHeader: failed to read header");

		String id = "";
		// check book type "MTIT" or "MTIU"
		for (int i = 0; i < ID_LENGTH; i++)
			id += (char) header[ID_OFFSET + i];

		if (id.equals(PDB_ID)) {
			encode = PDBEncode;
			bookType = BookType.pdb;
		} else if (id.equals(UPDB_ID)) {
			encode = UPDBEncode;
			bookType = BookType.updb;
		} else if (id.equals(PALMDOC_ID)) {
			encode = null;
			bookType = BookType.palmDoc;
		} else
			throw new Exception("readHeader: Unrecognized type id:" + id);


		//line records count
		recordCount = fromUInt16(header, RECODES_COUNT_OFFSET);

		//read all records offset
		recordOffsets = new Vector<Long>(recordCount);

		byte[] recordBuffer = new byte[8 * recordCount];
		if (is.read(recordBuffer) != recordBuffer.length)
			throw new IOException("readHeader: failed to read record info.");
		for (int i = 0; i < recordCount; i++)
			recordOffsets.add(fromUInt32(recordBuffer, i * 8));
	}

	public static int fromUInt16(byte buf[], int i)
	{
		int b1 = (0x000000FF & (int) buf[i]);
		int b2 = (0x000000FF & (int) buf[i + 1]);
		return (((b1 << 8) | b2));
	}

	public static long fromUInt32(byte buf[], int i)
	{
		int b1 = (0x000000FF & (int) buf[i]);
		int b2 = (0x000000FF & (int) buf[i + 1]);
		int b3 = (0x000000FF & (int) buf[i + 2]);
		int b4 = (0x000000FF & (int) buf[i + 3]);
		return ((long) ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4) & 0xFFFFFFFFL);
	}

	public static byte[] readRecord(VFile f, InputStream is, int recIndex) throws IOException, ArrayIndexOutOfBoundsException
	{
		if ((recIndex < 0) || (recIndex >= recordCount))
			throw new ArrayIndexOutOfBoundsException(
				"readRecord(" + recIndex + "): record index is out of bounds.");
		// Seek to the start of the given record
		// seek(recordOffsets.line(recIndex));

		// Compute lineCount of the record
		int recSize;
		if (recIndex < (recordCount - 1)) {
			// Record is not the last so its lineCount can be computed from the
			// starting offset of the following record.
			recSize = (int) (recordOffsets.get(recIndex + 1) - recordOffsets.get(recIndex));
		} else {
			// The last record in the DB occupies the rest of the space in the
			// file.
			recSize = (int) (f.length() - recordOffsets.get(recIndex));
		}
		// Read the record data
		byte[] recBytes = new byte[recSize];
		if (is.read(recBytes) != recSize) {
			throw new IOException("readRecord(" + recIndex + "): failed to read all bytes in record.");
		}

		return recBytes;
	}

	public boolean isBelong(VFile f)
	{
		for (String s : suffixes)
			if (f.getPath().toLowerCase().endsWith("." + s))
				return true;
		return false;
	}

	// this method get from http://gutenpalm.sourceforge.net/PalmIO/
	private static int decompress(byte[] in, byte[] out, int offset)
	{
		int i = 0;
		int j = offset;

		while (i < in.length) {
			// Get the next compressed input byte
			int c = ((int) in[i++]) & 0x00FF;

			if (c >= 0x00C0) {
				// type C command (space + char)
				out[j++] = ' ';
				out[j++] = (byte) (c & 0x007F);
			} else if (c >= 0x0080) {
				// type B command (sliding window sequence)

				// Move this to high bits and read low bits
				c = (c << 8) | (((int) in[i++]) & 0x00FF);
				// 3 + low 3 bits (Beirne's 'n'+3)
				int windowLen = 3 + (c & 0x0007);
				// next 11 bits (Beirne's 'm')
				int windowDist = (c >> 3) & 0x07FF;
				int windowCopyFrom = j - windowDist;

				windowLen = Math.min(windowLen, out.length - j);
				while (windowLen-- > 0)
					out[j++] = out[windowCopyFrom++];
			} else if (c >= 0x0009) {
				// self-representing, no command
				out[j++] = (byte) c;
			} else if (c >= 0x0001) {
				// type A command (next c chars are literal)
				c = Math.min(c, out.length - j);
				while (c-- > 0)
					out[j++] = in[i++];
			} else {
				// c == 0, also self-representing
				out[j++] = (byte) c;
			}
		}

		return j;
	}

	private void initPalmDocDB(byte[] rec)
	{
		compression = (rec[1] == 2);
		txtCount = fromUInt16(rec, TEXT_COUNT_OFFSET);
		TOC.add(new HaodooTOCR(null));
		recPos = 0;
	}

	// is java have such system method
	private static int searchByte(byte[] buf, int from, int to, byte v)
	{
		for (int i = from; i < to; i++)
			if (buf[i] == v)
				return i;
		return -1;
	}

	private static int formatPalmDocDB(byte[] buf, int size, HaodooTOCR ci) throws Exception
	{
		if (encode == null)
			encode = BookUtil.detect(new ByteArrayInputStream(buf, 0, size));

		int p = 0, np;
		while ((np = searchByte(buf, p, size, (byte) '\n')) >= 0) {
			ci.lines.add(new UString(new String(buf, p, np - p, encode)));
			p = np + 1;
		}

		int ret = size - p;
		if (ret > 0)
			System.arraycopy(buf, p, buf, 0, ret);

		return ret;
	}

	public Book load(VFile file, Config.ReadingInfo ri) throws Exception
	{
		encrypted = false;
		TOC.clear();

		InputStream is = file.getInputStream();
		readHeader(is);

		if (bookType == BookType.palmDoc)
			initPalmDocDB(readRecord(file, is, 0));
		else
			formatTitle(readRecord(file, is, 0));
		for (int i = 1; i <= txtCount; i++) {
			byte[] rec = readRecord(file, is, i);
			if (bookType == BookType.palmDoc) {
				if (recPos + MAX_REC_SIZE > recBuf.length) {
					byte[] ob = recBuf;
					recBuf = new byte[recBuf.length + MAX_REC_SIZE];
					System.arraycopy(ob, 0, recBuf, 0, recPos);
				}
				if (compression) {
					int cc = decompress(rec, recBuf, recPos);
					recPos = formatPalmDocDB(recBuf, cc, (HaodooTOCR) TOC.get(0));
				} else {
					System.arraycopy(rec, 0, recBuf, recPos, rec.length);
					recPos = formatPalmDocDB(recBuf, recPos + rec.length, (HaodooTOCR) TOC.get(0));
				}
			} else
				format(rec, (HaodooTOCR) TOC.get(i - 1));
		}
		is.close();

		if ((bookType == BookType.palmDoc) && (recPos > 0))
			formatPalmDocDB(recBuf, recPos, (HaodooTOCR) TOC.get(0));
		chapter = ri.chapter;
		content.setContent(((HaodooTOCR) TOC.get(chapter)).lines);
		return this;
	}

	@Override
	protected boolean loadChapter(int index)
	{
		chapter = index;
		content.setContent(((HaodooTOCR) TOC.get(index)).lines);
		return true;
	}

	@Override
	public Content content(int index)
	{
		return content;
	}

	@Override
	public void close()
	{
	}
}
