package zhang.lu.SimpleReader.vfs;

import zhang.lu.SimpleReader.Reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public abstract class VFile
{
	public static class Property
	{
		public String name;
		public boolean isFile;
		public long size;
	}

	public static final String CLOUD_FILE_PREFIX = "cloud:";

	private static String defaultEncode = "GBK";

	protected String path;

	public static VFile create(String path)
	{
		int np;
		if (isCloudFile(path))
			return new CloudFile(path);

		// local file
		File rf;
		String fullpath = Reader.pathPrefix + path;
		np = 0;
		try {
			while ((np = fullpath.toLowerCase().indexOf(".zip/", np)) >= 0) {
				rf = new File(fullpath.substring(0, np + 4));
				if (!rf.exists())
					throw new FileNotFoundException();
				if (rf.isFile())
					return new ZipBasedFile(path, defaultEncode);
			}
			if (fullpath.toLowerCase().endsWith(".zip"))
				if (new File(fullpath).isFile())
					return new ZipBasedFile(path, defaultEncode);
		} catch (IOException e) {
			return new RealFile(path);
		}
		return new RealFile(path);
	}

	public static boolean isCloudFile(String path)
	{
		return path != null && path.indexOf(CLOUD_FILE_PREFIX) == 0;
	}

	public static void setDefaultEncode(String encode)
	{
		defaultEncode = encode;
	}

	public static String getDefaultEncode()
	{
		return defaultEncode;
	}

	public String getPathPrefix()
	{
		return Reader.pathPrefix;
	}

	protected VFile(String aPath)
	{
		path = aPath;
	}

	public abstract boolean exists();

	public List<Property> listProperty()
	{
		return listProperty(true);
	}

	public String getRealPath()
	{
		return getPathPrefix() + getPath();
	}

	public String getPath()
	{
		return path;
	}

	public abstract List<Property> listProperty(boolean needDir);

	public abstract boolean isDirectory();

	public abstract boolean isHidden();

	public abstract long length();

	public abstract InputStream getInputStream() throws IOException;
}
