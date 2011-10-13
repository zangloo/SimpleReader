package zhang.lu.SimpleReader.VFS;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public class CloudFile extends VFile
{
	private static final String GET_LIST_PHP = "/bookfeed/getlist.php";
	private static final String GET_PROPERTY_PHP = "/bookfeed/getproperty.php";

	private static final String PARAM_PATH = "path";

	private boolean init = false;
	private Property property = null;

	protected CloudFile(String aPath)
	{
		super(aPath);
		path = aPath.substring(CLOUD_FILE_PREFIX.length());
	}

	@Override
	public boolean exists()
	{
		if (!init)
			property = retrieveProperty(path);

		return property != null;
	}

	@Override
	public List<Property> listProperty(boolean needDir)
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));

		InputStream in;
		try {
			in = getResponse(GET_LIST_PHP, p);
			return parserList(in, needDir);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean isDirectory()
	{
		if (!init)
			property = retrieveProperty(path);

		return (property != null) && (!property.isFile);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}

	@Override
	public long length()
	{
		if (!init)
			property = retrieveProperty(path);

		return property == null ? 0 : property.size;
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return null;
	}

	@Override
	public String getRealPath()
	{
		return getPath();
	}

	@Override
	public String getPathPrefix()
	{
		return CLOUD_FILE_PREFIX;
	}

	public static InputStream getResponse(String file, ArrayList<NameValuePair> params) throws Exception
	{
		URI uri = URIUtils
			.createURI("http", CloudServerAddr, -1, file, URLEncodedUtils.format(params, "UTF-8"), null);
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null)
			return entity.getContent();
		else
			return null;
	}

	public static List<Property> parserList(InputStream in, boolean needDir) throws IOException
	{
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		if (jp.nextToken() != JsonToken.START_ARRAY)
			throw new IOException("invalid data");
		ArrayList<Property> ps = new ArrayList<Property>();
		while (jp.nextToken() == JsonToken.START_OBJECT) {
			Property p = new Property();
			while (jp.nextToken() == JsonToken.FIELD_NAME) {
				String fn = jp.getCurrentName();
				JsonToken jt = jp.nextToken();
				if ("name".equals(fn)) {
					if (jt != JsonToken.VALUE_STRING)
						throw new IOException("invalid data");
					p.name = jp.getText();
				} else if ("isfile".equals(fn))
					p.isFile = (jt == JsonToken.VALUE_TRUE);
				else if ("size".equals(fn)) {
					if (jt != JsonToken.VALUE_NUMBER_INT)
						throw new IOException("invalid data");
					p.size = new Integer(jp.getText());
				}
			}
			if (p.isFile || needDir)
				ps.add(p);
		}
		jp.close();
		return ps;
	}

	public static Property parserProperty(InputStream in) throws IOException
	{
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		if (jp.nextToken() != JsonToken.START_OBJECT)
			throw new IOException("invalid data");
		Property p = new Property();
		while (jp.nextToken() == JsonToken.FIELD_NAME) {
			String fn = jp.getCurrentName();
			JsonToken jt = jp.nextToken();
			if ("name".equals(fn)) {
				if (jt != JsonToken.VALUE_STRING)
					throw new IOException("invalid data");
				p.name = jp.getText();
			} else if ("isfile".equals(fn))
				p.isFile = (jt == JsonToken.VALUE_TRUE);
			else if ("size".equals(fn)) {
				if (jt != JsonToken.VALUE_NUMBER_INT)
					throw new IOException("invalid data");
				p.size = new Integer(jp.getText());
			}
		}
		jp.close();
		return p;
	}

	public static Property retrieveProperty(String path)
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));

		InputStream in;
		try {
			in = getResponse(GET_PROPERTY_PHP, p);
			return parserProperty(in);
		} catch (Exception e) {
			return null;
		}
	}
}
