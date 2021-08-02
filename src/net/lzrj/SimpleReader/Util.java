package net.lzrj.SimpleReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.webkit.WebView;

/**
 * Created by IntelliJ IDEA.
 * User: zhanglu
 * Date: 10-12-5
 * Time: 下午8:22
 */
public class Util
{
	static public void errorMsg(Context activity, int msgID)
	{
		new AlertDialog.Builder(activity).setTitle(R.string.error).setMessage(msgID)
						 .setPositiveButton(R.string.button_ok_text, null).show();
	}

	static public void errorMsg(Context activity, String msg)
	{
		new AlertDialog.Builder(activity).setTitle(R.string.error).setMessage(msg)
						 .setPositiveButton(R.string.button_ok_text, null).show();
	}

	static public void showDialog(Context activity, String msg, int titleID)
	{
		WebView wv = new WebView(activity);
		wv.loadDataWithBaseURL(null, msg, "text/html", "utf-8", null);
		new AlertDialog.Builder(activity).setTitle(titleID).setView(wv)
						 .setPositiveButton(R.string.button_ok_text, null).show();
	}

	static public void setActivityOrient(Activity activity, int viewOrient)
	{
		switch (viewOrient) {
			case Configuration.ORIENTATION_LANDSCAPE:
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			default:
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

	}
}
