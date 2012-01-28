package zhang.lu.SimpleReader.Book;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 12-1-28
 * Time: 上午11:09
 */
public class TOCRecord
{
	protected String title;

	public TOCRecord(String t) {title = t;}

	public String title() {return title;}

	public int level() { return 0;}
}

