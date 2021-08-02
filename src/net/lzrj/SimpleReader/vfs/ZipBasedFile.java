package net.lzrj.SimpleReader.vfs;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public class ZipBasedFile extends VFile
{
	private ZipFile rzf;
	private File rf;
	private String cp;
	private String encode;
	private ZipArchiveEntry zae = null;

	protected ZipBasedFile(String pathname, String aEncode) throws IOException
	{
		super(pathname);
		encode = aEncode;
		pathname = getRealPath();

		int np = 0;
		while ((np = pathname.toLowerCase().indexOf(".zip/", np)) >= 0) {
			rf = new File(pathname.substring(0, np + 4));
			if (!rf.exists())
				throw new FileNotFoundException();
			if (rf.isFile()) {
				rzf = new ZipFile(rf, encode);
				cp = pathname.substring(np + 5);
				if (cp.length() > 0)
					if ((zae = rzf.getEntry(cp)) == null)
						if ((zae = rzf.getEntry(cp + "/")) == null)
							throw new FileNotFoundException();
						else
							cp += "/";
				return;
			}
		}
		if (pathname.toLowerCase().endsWith(".zip")) {
			rf = new File(pathname);
			if (rf.isFile()) {
				rzf = new ZipFile(rf, encode);
				cp = "";
			}
		}
	}

	@Override
	public boolean exists()
	{
		try {
			ZipFile zf = new ZipFile(rf, encode);
			Enumeration es = zf.getEntries();
			if (cp.length() > 0)
				while (es.hasMoreElements())
					if (cp.equals(((ZipArchiveEntry) es.nextElement()).getName()))
						return true;
			return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public List<Property> listProperty(boolean needDir)
	{
		if (!isDirectory())
			return null;

		Property p;
		List<Property> ps = new ArrayList<Property>();

		try {
			ZipFile zf = new ZipFile(rf, encode);
			Enumeration es = zf.getEntries();
			if (cp.length() > 0)
				while (es.hasMoreElements())
					if (cp.equals(((ZipArchiveEntry) es.nextElement()).getName()))
						break;

			while (es.hasMoreElements()) {
				ArchiveEntry ae = (ZipArchiveEntry) es.nextElement();
				if (!ae.getName().startsWith(cp))
					break;
				String n = ae.getName().substring(cp.length());
				int pos = n.indexOf('/');
				if (pos < 0) {
					p = new Property();
					p.isFile = true;
					p.size = ae.getSize();
				} else if (needDir) {
					if (pos < (n.length() - 1))
						continue;
					p = new Property();
					n = n.substring(0, pos);
					p.isFile = false;
					p.size = 0;
				} else
					continue;
				p.name = n;
				ps.add(p);
			}
			Collections.sort(ps, new Comparator<Property>()
			{
				@Override
				public int compare(Property p1, Property p2)
				{
					return p1.name.compareTo(p2.name);
				}
			});
			return ps;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public boolean isDirectory()
	{
		return (cp.length() == 0) || cp.endsWith("/");
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}

	@Override
	public long length()
	{
		if (isDirectory())
			return 0;

		return rzf.getEntry(cp).getSize();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return new VInputStream(rzf.getInputStream(zae));
	}
}
