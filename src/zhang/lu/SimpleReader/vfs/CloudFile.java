package zhang.lu.SimpleReader.vfs;

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
import zhang.lu.SimpleReader.book.SRBOnline.SRBOnlineLoader;
import zhang.lu.SimpleReader.book.TOCRecord;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-13
 * Time: 下午2:21
 */
public class CloudFile extends VFile
{
	public static class OnlineProperty extends Property
	{
		public boolean hasNotes;
		public char mark;
		public int indexBase;
	}

	private static final String GET_LIST_PHP = "/bookfeed/getlist.php";
	private static final String GET_PROPERTY_PHP = "/bookfeed/getprop.php";
	private static final String GET_CHAPTERS_PHP = "/bookfeed/getchapters.php";
	private static final String GET_LINES_PHP = "/bookfeed/getlines.php";
	private static final String GET_NOTES_PHP = "/bookfeed/getnotes.php";
	private static final String CloudServerAddr = "simplereader.sourceforge.net";
	private static final String PARAM_PATH = "path";
	private static final String PARAM_CIDX = "cidx";

	private boolean init = false;
	private OnlineProperty property = null;

	protected CloudFile(String aPath)
	{
		super(aPath);
		path = aPath.substring(CLOUD_FILE_PREFIX.length());
	}

	@Override
	public boolean exists()
	{
		if (!init)
			property = retrieveProperty();

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
			property = retrieveProperty();

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
			property = retrieveProperty();

		return property == null ? 0 : property.size;
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		throw new IOException("Not supported");
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

	public static InputStream getResponse(String file, ArrayList<NameValuePair> params) throws IOException, URISyntaxException
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

	private static void checkNextToken(JsonParser jp, JsonToken value) throws IOException
	{
		if (jp.nextToken() != value)
			throw new IOException("invalid data");
	}

	public static List<Property> parserList(InputStream in, boolean needDir) throws IOException
	{
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		checkNextToken(jp, JsonToken.START_ARRAY);
		ArrayList<Property> ps = new ArrayList<Property>();
		while (jp.nextToken() == JsonToken.START_OBJECT) {
			Property p = new Property();
			while (jp.nextToken() == JsonToken.FIELD_NAME) {
				String fn = jp.getCurrentName();
				if ("name".equals(fn))
					p.name = jp.nextTextValue();
				else if ("isfile".equals(fn))
					p.isFile = jp.nextBooleanValue();
				else if ("size".equals(fn))
					p.size = jp.nextIntValue(10);
			}
			if (p.isFile || needDir)
				ps.add(p);
		}
		jp.close();
		return ps;
	}

	private OnlineProperty retrieveProperty()
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));

		InputStream in;
		try {
			in = getResponse(GET_PROPERTY_PHP, p);
			JsonFactory f = new JsonFactory();
			JsonParser jp = f.createJsonParser(in);

			checkNextToken(jp, JsonToken.START_OBJECT);
			OnlineProperty prop = new OnlineProperty();
			while (jp.nextToken() == JsonToken.FIELD_NAME) {
				String fn = jp.getCurrentName();
				if ("name".equals(fn))
					prop.name = jp.nextTextValue();
				else if ("isfile".equals(fn))
					prop.isFile = jp.nextBooleanValue();
				else if ("size".equals(fn))
					prop.size = jp.nextIntValue(10);
				else if ("noteMark".equals(fn))
					prop.mark = jp.nextTextValue().charAt(0);
				else if ("hasNotes".equals(fn))
					prop.hasNotes = jp.nextBooleanValue();
				else if ("indexBase".equals(fn))
					prop.indexBase = jp.nextIntValue(10);
			}
			jp.close();
			return prop;
		} catch (IOException e1) {
			return null;
		} catch (URISyntaxException e1) {
			return null;
		}
	}

	public ArrayList<TOCRecord> getChapters() throws IOException, URISyntaxException
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));

		InputStream in = getResponse(GET_CHAPTERS_PHP, p);
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		checkNextToken(jp, JsonToken.START_ARRAY);
		ArrayList<TOCRecord> cs = new ArrayList<TOCRecord>();
		while (jp.nextToken() == JsonToken.VALUE_STRING)
			cs.add(new SRBOnlineLoader.OnlineTOC(jp.getText()));
		jp.close();
		return cs;
	}

	public ArrayList<String> getLines(int cidx) throws IOException, URISyntaxException
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));
		p.add(new BasicNameValuePair(PARAM_CIDX, String.valueOf(cidx)));

		// server will compress data
		InputStream in = new InflaterInputStream(getResponse(GET_LINES_PHP, p));
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		checkNextToken(jp, JsonToken.START_ARRAY);
		ArrayList<String> ss = new ArrayList<String>();
		while (jp.nextToken() == JsonToken.VALUE_STRING)
			ss.add(jp.getText());
		jp.close();
		return ss;
	}

	public HashMap<Long, String> getNotes(int index) throws IOException, URISyntaxException
	{
		ArrayList<NameValuePair> p = new ArrayList<NameValuePair>();
		p.add(new BasicNameValuePair(PARAM_PATH, path));
		p.add(new BasicNameValuePair(PARAM_CIDX, String.valueOf(index)));

		// server will compress data
		InputStream in = new InflaterInputStream(getResponse(GET_NOTES_PHP, p));
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		checkNextToken(jp, JsonToken.START_OBJECT);
		HashMap<Long, String> notes = new HashMap<Long, String>();
		while (jp.nextToken() == JsonToken.FIELD_NAME) {
			int line = Integer.parseInt(jp.getText());
			checkNextToken(jp, JsonToken.START_OBJECT);
			while (jp.nextToken() == JsonToken.FIELD_NAME) {
				int offset = Integer.parseInt(jp.getText());
				checkNextToken(jp, JsonToken.VALUE_STRING);
				String note = jp.getText();

				SRBOnlineLoader.OnlineTOC.addNote(notes, line, offset, note);
			}
		}
		jp.close();
		return notes;
	}

	public OnlineProperty getProperty()
	{
		if (property == null)
			property = retrieveProperty();
		return property;
	}
}
