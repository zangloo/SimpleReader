package zhang.lu.SimpleReader.VFS;

import zhang.lu.SimpleReader.Config;
import zhang.lu.SimpleReader.Reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public class RealFile extends VFile
{
	File rf;

	protected RealFile(String path)
	{
		super(path);
		rf = new File(getRealPath());
	}

	@Override
	public boolean exists()
	{
		return rf.exists();
	}

	@Override
	public List<Property> listProperty(boolean needDir)
	{
		String[] list;
		Property p;
		List<Property> ps = new ArrayList<Property>();

		if (!rf.exists())
			return null;
		if ((list = rf.list()) == null)
			return ps;
		for (String n : list) {
			if (n.equals(Config.configDB))
				continue;
			if (n.endsWith(Reader.dictSuffix))
				continue;
			if (n.endsWith(Reader.fontSuffix))
				continue;
			VFile f = create(getPath() + "/" + n);
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

	@Override
	public boolean isDirectory()
	{
		return rf.isDirectory();
	}

	@Override
	public boolean isHidden()
	{
		return rf.isHidden();
	}

	@Override
	public long length()
	{
		return rf.length();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return new FileInputStream(rf);
	}
}
