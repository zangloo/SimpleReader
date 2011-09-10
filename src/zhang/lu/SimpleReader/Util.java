package zhang.lu.SimpleReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

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

	static public void showNote(Context activity, String msg)
	{
		new AlertDialog.Builder(activity).setTitle(R.string.note_title).setMessage(msg)
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
