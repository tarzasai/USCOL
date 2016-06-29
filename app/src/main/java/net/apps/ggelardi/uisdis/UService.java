package net.apps.ggelardi.uisdis;

import android.app.IntentService;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class UService extends IntentService implements ShakeDetector.Listener {
	public static final String ACT_STATUS_IDLE = "UService.STATUS_IDLE";
	public static final String ACT_HANDLE_CALL = "UService.HANDLE_CALL";
	public static final String ACT_BEGIN_TEST = "UService.BEGIN_TEST";
	public static final String ACT_END_TEST = "UService.END_TEST";

	private static final String TAG = "UService";

	private USession session;
	private SensorManager sensorManager;
	private ShakeDetector shakeDetector;

	public UService() {
		super("UService");

		session = USession.getInstance(this);
		shakeDetector = new ShakeDetector(this);
	}

	@Override
	public void hearShake() {
		shakeDetector.stop();
		lowerRingVolume();
		String num = session.getLastCallNumber();
		if (num == null)
			return;
		// unlock
		/*
		//Get the window from the context
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		//Unlock
		//http://developer.android.com/reference/android/app/Activity.html#getWindow()
		Window window = this.getWindow();
		window.addFlags(wm.LayoutParams.FLAG_DISMISS_KEYGUARD);
		*/
		// search
		Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
		si.putExtra(SearchManager.QUERY, "\"" + num + "\"");
		si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(si);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Log.d(TAG, intent.toString());
		String act = intent.getAction();
		if (act.equals(ACT_STATUS_IDLE)) {
			shakeDetector.stop();
			resetRingVolume();
		} else if (act.equals(ACT_HANDLE_CALL)) {
			String num = session.getLastCallNumber();
			if (num == null) {
				if (session.getRejectPrivateNumCalls() && killCall())
					Toast.makeText(getApplicationContext(), R.string.endcall_private_toast, Toast.LENGTH_LONG).show();
			} else if (!contactExists(num))
				shakeDetector.start(sensorManager);
		} else if (act.equals(ACT_BEGIN_TEST)) {
			session.setLastCallNumber("3472002591");
			shakeDetector.start(sensorManager);
		} else if (act.equals(ACT_END_TEST)) {
			session.setLastCallNumber(null);
			shakeDetector.stop();
		}
	}

	private void lowerRingVolume() {
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		//am.getStreamVolume(AudioManager.STREAM_RING)

		SharedPreferences prefs = session.getPrefs();
	}

	private void resetRingVolume() {
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		//am.getStreamVolume(AudioManager.STREAM_RING)
	}

	private void doSearch() {
		shakeDetector.stop();
		lowerRingVolume();
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
