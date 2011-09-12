package zhang.lu.SimpleReader.Book;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import zhang.lu.SimpleReader.Config;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public class VFile extends File
{
	private static String defaultEncode = "GBK";

	private boolean v = false;
	private ZipFile rzf;
	private File rf;
	private String cp;
	private String encode = defaultEncode;
	private ZipArchiveEntry zae = null;

	public class Property
	{
		public String name;
		public boolean isFile;
		public long size;
	}

	public VFile(String pathname)
	{
		this(pathname, defaultEncode);
	}

	public VFile(String pathname, String aEncode)
	{
		super(pathname);
		encode = aEncode;

		try {
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
					v = true;
					return;
				}
			}
			if (pathname.toLowerCase().endsWith(".zip")) {
				rf = new File(pathname);
				if (rf.isFile()) {
					rzf = new ZipFile(rf, encode);
					cp = "";
					v = true;
				}
			}
		} catch (IOException e) {
			v = false;
		}
	}

	@Override
	public String[] list()
	{
		if (!v)
			return super.list();

		if (!isDirectory())
			return null;

		try {
			ZipFile zf = new ZipFile(rf, encode);
			Enumeration es = zf.getEntries();
			if (cp.length() > 0)
				while (es.hasMoreElements())
					if (cp.equals(((ZipArchiveEntry) es.nextElement()).getName()))
						break;

			ArrayList<String> l = new ArrayList<String>();
			while (es.hasMoreElements()) {
				ArchiveEntry ae = (ZipArchiveEntry) es.nextElement();
				if (!ae.getName().startsWith(cp))
					break;
				String n = ae.getName().substring(cp.length());
				int p = n.indexOf('/');
				if (p >= 0) {
					if (p < (n.length() - 1))
						continue;
					n = n.substring(0, p);
				}
				l.add(n);
			}

			return l.toArray(new String[l.size()]);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public boolean exists()
	{
		if (!v)
			return super.exists();

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

	public List<Property> listProperty()
	{
		return listProperty(true);
	}

	public List<Property> listProperty(boolean needDir)
	{
		if (!isDirectory())
			return null;

		String[] list;
		Property p;
		String path = getPath();
		List<Property> ps = new ArrayList<Property>();

		if (!v) {
			list = list();
			for (String n : list) {
				if (n.equals(Config.configDB))
					continue;
				if (n.endsWith(zhang.lu.SimpleReader.Reader.dictSuffix))
					continue;
				VFile f = new VFile(path + "/" + n);
				if (f.isHidden())
					continue;
				if (!f.isDirectory()) {
					p = new Property();
					p.isFile = true;
					p.size = f.length();
				} else if (needDir) {
					p = new Property();
					p.isFile = false;
					p.size = 0;
				} else
					continue;
				p.name = n;
				ps.add(p);
			}
			return ps;
		}

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
		if (!v)
			return super.isDirectory();

		return (cp.length() == 0) || cp.endsWith("/");
	}

	@Override
	public boolean isHidden()
	{
		return (!v) && super.isHidden();
	}

	@Override
	public long length()
	{
		if (!v)
			return super.length();

		if (isDirectory())
			return 0;

		return rzf.getEntry(cp).getSize();
	}

	public InputStream getInputStream() throws IOException
	{
		return (v) ? new VInputStream(rzf.getInputStream(zae)) : new FileInputStream(this);
	}

	public static void setDefaultEncode(String encode)
	{
		defaultEncode = encode;
	}

	public static String getDefaultEncode()
	{
		return defaultEncode;
	}
}
