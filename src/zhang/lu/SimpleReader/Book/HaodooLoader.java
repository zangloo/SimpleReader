package zhang.lu.SimpleReader.Book;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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
public class HaodooLoader implements BookLoader.Loader
{
	private static final String[] suffixes = {"pdb", "updb"};

	public static final int HEADER_LENGTH = 78;
	public static final String TYPE_ID = "BOOK";
	public static final int TYPE_ID_OFFSET = 60;
	public static final String PDB_ID = "MTIT";
	public static final String UPDB_ID = "MTIU";
	public static final String PDB_TITLE_SEPARATOR = "\u001b";
	public static final String UPDB_TITLE_SEPARATOR = "\r\n";

	public static final int RECODES_COUNT_OFFSET = 76;
	public static final int ID_OFFSET = 64;
	public static final int ID_LENGTH = 4;

	//"★★★★★★★以下內容★★︽本版︾★★無法顯示★★★★★★★";
	public static final byte[] ENCRYPT_MARK = {(byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA5, (byte) 0x48, (byte) 0xA4, (byte) 0x55, (byte) 0xA4, (byte) 0xBA, (byte) 0xAE, (byte) 0x65, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0x6F, (byte) 0xA5, (byte) 0xBB, (byte) 0xAA, (byte) 0xA9, (byte) 0xA1, (byte) 0x70, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xB5, (byte) 0x4C, (byte) 0xAA, (byte) 0x6B, (byte) 0xC5, (byte) 0xE3, (byte) 0xA5, (byte) 0xDC, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0xA1, (byte) 0xB9, (byte) 0x0D, (byte) 0x0A};

	private int recordCount;
	private Vector<Long> recordOffsets;
	String encode;
	boolean encrypted = false;

	List<String> text;
	InputStream is;
	VFile f;

	protected static String PDBEncode = "BIG5";
	protected static String UPDBEncode = "UTF-16LE";

	private void unEncrypt(byte[] rec)
	{
		for (int i = 1; i < rec.length; i += 2)
			if (rec[i] != 0x0a)
				rec[i]--;
	}

	private void formatTitle(byte[] rec)
	{
		String s;
		int len = (PDBEncode.equals(encode)) ? rec.length - 1 - 8 : rec.length - 8;
		String separator = (PDBEncode.equals(encode)) ? PDB_TITLE_SEPARATOR : UPDB_TITLE_SEPARATOR;
		try {
			s = new String(rec, 8, len, encode);
		} catch (UnsupportedEncodingException e) {
			return;
		}
		//title
		int np = s.indexOf(0x1b);
		text.add(s.substring(0, np));

		int p = s.indexOf(0x1b, np + 3) + 1;
		while ((np = s.indexOf(separator, p)) >= 0) {
			text.add(s.substring(p, np));
			p = np + separator.length();
		}

		if (p < s.length())
			text.add(s.substring(p));

		text.add("");
	}

	private void format(byte[] rec)
	{
		String s;
		int offset = 0;
		int len = (PDBEncode.equals(encode)) ? rec.length - 1 : rec.length - 2;
		try {
			// updb has no encrypted, check pdb only
			if (PDBEncode.equals(encode)) {
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
					unEncrypt(rec);
			}
			s = new String(rec, offset, len, encode);
		} catch (UnsupportedEncodingException e) {
			return;
		}

		int p = 0, np;
		while ((np = s.indexOf("\r\n", p)) >= 0) {
			text.add(s.substring(p, np));
			p = np + 2;
		}

		if (p < s.length())
			text.add(s.substring(p));

		text.add("");
	}

	private String readHeader() throws IOException
	{
		byte[] header = new byte[HEADER_LENGTH];

		if (is.read(header) != HEADER_LENGTH)
			throw new IOException("readHeader: failed to read header");

		// check type id "BOOK"
		String id = "";
		for (int i = 0; i < TYPE_ID.length(); i++)
			id += header[TYPE_ID_OFFSET + i];
		if (TYPE_ID.equals(id))
			throw new IOException("readHeader: Unrecognized type id:" + id);

		// check book type "MTIT" or "MTIU"
		id = "";
		for (int i = 0; i < ID_LENGTH; i++)
			id += (char) header[ID_OFFSET + i];

		String encode;
		if (id.equals(PDB_ID))
			encode = PDBEncode;
		else if (id.equals(UPDB_ID))
			encode = UPDBEncode;
		else
			encode = PDBEncode;

		//line records count
		recordCount = (header[RECODES_COUNT_OFFSET] << 8) + header[RECODES_COUNT_OFFSET + 1];

		//read all records offset
		recordOffsets = new Vector<Long>(recordCount);

		byte[] recordBuffer = new byte[8 * recordCount];
		if (is.read(recordBuffer) != recordBuffer.length)
			throw new IOException("readHeader: failed to read record info.");
		for (int i = 0; i < recordCount; i++)
			recordOffsets.add(fromUInt32(recordBuffer, i * 8));

		return encode;
	}

	public static long fromUInt32(byte buf[], int i)
	{
		int b1 = (0x000000FF & (int) buf[i]);
		int b2 = (0x000000FF & (int) buf[i + 1]);
		int b3 = (0x000000FF & (int) buf[i + 2]);
		int b4 = (0x000000FF & (int) buf[i + 3]);
		return ((long) ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4) & 0xFFFFFFFFL);
	}

	public byte[] readRecord(int recIndex) throws IOException, ArrayIndexOutOfBoundsException
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


	public String[] getSuffixes()
	{
		return suffixes;
	}

	public BookContent load(VFile file) throws Exception
	{
		encrypted = false;

		f = file;
		text = new ArrayList<String>();

		is = f.getInputStream();
		encode = readHeader();
		formatTitle(readRecord(0));
		for (int i = 1; i < recordCount - 1; i++)
			format(readRecord(i));
		is.close();
		return new PlainTextContent(text);
	}

	public void unload(BookContent aBook)
	{
	}
}
