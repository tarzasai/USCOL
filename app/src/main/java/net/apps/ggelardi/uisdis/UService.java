package net.apps.ggelardi.uisdis;

import android.app.IntentService;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Method;

public class UService extends IntentService implements ShakeDetector.Listener {
	public static final String ACT_STATUS_IDLE = "UService.STATUS_IDLE";
	public static final String ACT_INC_PRIVATE = "UService.INC_PRIVATE";
	public static final String ACT_INC_WITHNUM = "UService.INC_WITHNUM";
	public static final String ACT_REQ_SEARCH = "UService.REQ_SEARCH";
	public static final String ACT_REQ_HANGUP = "UService.REQ_HANGUP";
	//
	public static final String ACT_TEST_SHAKE1 = "UService.TEST_SHAKE1";
	public static final String ACT_TEST_SHAKE2 = "UService.TEST_SHAKE2";
	//
	public static final String PRM_RCVD_NUMBER = "NUMBER";

	private static final String TAG = "UService";

	private USession session;
	private SensorManager sensorManager;
	private ShakeDetector shakeDetector;
	private String lastNumber = null;

	public UService() {
		super("UService");

		session = USession.getInstance(this);
		shakeDetector = new ShakeDetector(this);
	}

	@Override
	public void hearShake() {
		//TODO: abbassa il volume della suoneria
		if (lastNumber != null) {

			//Get the window from the context
			WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
			//Unlock
			//http://developer.android.com/reference/android/app/Activity.html#getWindow()
			Window window = this.getWindow();
			window.addFlags(wm.LayoutParams.FLAG_DISMISS_KEYGUARD);


			Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
			si.putExtra(SearchManager.QUERY, "\"" + lastNumber + "\"");
			si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(si);
			lastNumber = null;
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Log.d(TAG, intent.toString());
		String act = intent.getAction();
		if (act.equals(ACT_STATUS_IDLE)) {
			shakeDetector.stop();
		} else if (act.equals(ACT_INC_WITHNUM)) {
			lastNumber = intent.getStringExtra(PRM_RCVD_NUMBER);
			if (!contactExists(lastNumber))
				shakeDetector.start(sensorManager);
		} else if (act.equals(ACT_INC_PRIVATE)) {
			if (session.getRejectPrivateNumCalls() && killCall())
				Toast.makeText(getApplicationContext(), R.string.endcall_private_toast, Toast.LENGTH_LONG).show();
		} else if (act.equals(ACT_REQ_SEARCH)) {
			shakeDetector.stop();
			String num = intent.getStringExtra(PRM_RCVD_NUMBER);

			/*
			Intent si = new Intent(this, SearchActivity.class);
			si.putExtra(PRM_RCVD_NUMBER, num);
			si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(si);
			*/

			Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
			si.putExtra(SearchManager.QUERY, "\"" + num + "\"");
			si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(si);

		} else if (act.equals(ACT_REQ_HANGUP)) {
			shakeDetector.stop();
			lastNumber = null;
			Toast.makeText(getApplicationContext(), R.string.endcall_action_toast, Toast.LENGTH_LONG).show();
		} else if (act.equals(ACT_TEST_SHAKE1)) {
			shakeDetector.start(sensorManager);
		} else if (act.equals(ACT_TEST_SHAKE2)) {
			shakeDetector.stop();
		}
	}

	private boolean contactExists(String phoneNumber) {
		Log.d(TAG, "Looking up for contact with number " + phoneNumber);
		Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor cur = this.getContentResolver().query(uri, new String[]{
			ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
		try {
			if (cur.moveToFirst()) {
				String name = cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
				Log.d(TAG, "Contact found: " + name);
				return true;
			}
		} catch (Exception err) {
			Log.e(TAG, "contactExists()", err);
			return false;
		} finally {
			if (cur != null)
				cur.close();
		}
		Log.d(TAG, "Unknown caller");
		return false;
	}

	private boolean onWIFI() {
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		int ns = (ni != null && ni.isConnected()) ? ni.getType() : ConnectivityManager.TYPE_DUMMY;
		return ns == ConnectivityManager.TYPE_WIFI;
	}

	private boolean killCall() {
		try {
			TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
			// get the getITelephony() method
			Class cTelephonyManager = Class.forName(tm.getClass().getName());
			Method mGetITelephony = cTelephonyManager.getDeclaredMethod("getITelephony");
			mGetITelephony.setAccessible(true); // is supposed to be private
			// invoke getITelephony() to get the ITelephony interface
			Object iITelephony = mGetITelephony.invoke(tm);
			// get the endCall method from ITelephony
			Class cTelephony = Class.forName(iITelephony.getClass().getName());
			Method mEndCall = cTelephony.getDeclaredMethod("endCall");
			// invoke endCall()
			mEndCall.invoke(iITelephony);
		} catch (Exception err) {
			Log.e(TAG, "killCall()", err);
			return false;
		}
		Log.d(TAG, "Call ended");
		return true;
	}

	private void showSearchPrompt(String num) {
		//TODO: themed activity http://stackoverflow.com/a/7918720/28852
		// https://www.youtube.com/watch?v=kjGPE_XLmwg
	}
}
