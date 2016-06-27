package net.apps.ggelardi.uisdis;

import android.app.AlertDialog;
import android.app.IntentService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class UService extends IntentService {
	public static final String ACT_NO_NUMBER = "UService.NO_NUMBER";
	public static final String ACT_WITH_NUMBER = "UService.WITH_NUMBER";
	public static final String PRM_INCOMING_NUMBER = "UService.INCOMING_NUMBER";

	private static final String TAG = "UService";

	private USession session;

	public UService() {
		super("UService");

		session = USession.getInstance(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.getAction().equals(ACT_WITH_NUMBER)) {
			String num = intent.getStringExtra(PRM_INCOMING_NUMBER);
			if (!contactExists(num)) {
				Log.d(TAG, "Unknown caller, let's google it...");
				if (session.getOnlySearchOnWiFi()) {
					ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo ni = cm.getActiveNetworkInfo();
					int ns = (ni != null && ni.isConnected()) ? ni.getType() : ConnectivityManager.TYPE_DUMMY;
					if (ns != ConnectivityManager.TYPE_WIFI) {
						Log.d(TAG, "Actually we can't, we are not on WIFI.");
						return;
					}
				}
				showSearchPrompt(num);
				/*
				Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
				intent.putExtra(SearchManager.SUGGEST_URI_PATH_QUERY, "\"" + num + "\"");
				startActivity(intent);
				*/
			}
		} else if (intent.getAction().equals(ACT_NO_NUMBER) && session.getRejectPrivateNumCalls()) {
			Log.i(TAG, "Private number: attempt to end the incoming call by user preferences...");
			//killCall();
		}
	}

	private boolean contactExists(String phoneNumber) {
		Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor cur = this.getContentResolver().query(uri, new String[]{
			ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
		try {
			if (cur.moveToFirst()) {
				String name = cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
				Log.d(TAG, "Caller is known: " + name);
				return true;
			}
		} catch (Exception err) {
			Log.e(TAG, "dsad", err);
			return false;
		} finally {
			if (cur != null)
				cur.close();
		}
		return false;
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
			Log.e(TAG, "Unable to end the call", err);
			return false;
		}
		return true;
	}

	private void showSearchPrompt(String num) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this.getApplicationContext());
		bld.setTitle("Unknown caller!");
		bld.setMessage(num);
		bld.setCancelable(true);
		bld.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				//
			}
		});
		bld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				//
			}
		});
		AlertDialog dlg = bld.show();
		dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

		// oppure https://www.youtube.com/watch?v=kjGPE_XLmwg
	}
}
