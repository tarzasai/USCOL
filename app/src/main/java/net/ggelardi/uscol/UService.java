package net.ggelardi.uscol;

import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.lang.reflect.Method;

public class UService extends IntentService implements ShakeDetector.Listener {
	public static final String ACT_OPEN_LOG = "UService.OPEN_LOG";
	public static final String ACT_TEST_START = "UService.TEST_START";
	public static final String ACT_TEST_STOP = "UService.TEST_STOP";

	private static final String TAG = "UService";

	private USession session;
	private ShakeDetector shakede;

	public UService() {
		super("UService");

		session = USession.getInstance(this);
		shakede = new ShakeDetector(this);
	}

	@Override
	public void hearShake() {
		//Toast.makeText(getApplicationContext(), "SHAKE THAT ASS!", Toast.LENGTH_SHORT).show();
		shakede.stop();
		session.lowerRingVolume();
		String num = session.getLastIncomingNumber();
		if (num == null || (!onWIFI() && session.getOnlySearchOnWiFi()))
			return;
		// unlock
		KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
		if (km.inKeyguardRestrictedInputMode()) {
			KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock(TAG);
			keyguardLock.disableKeyguard();
		}
		// remember
		session.setLastSearchedNumber(num);
		// remove country code
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber pn = pu.parse(num, "");
			if (pn.hasNationalNumber())
				num = String.valueOf(pn.getNationalNumber());
		} catch (NumberParseException e) {
			Log.e(TAG, "hearShake", e);
		}
		// search
		Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
		si.putExtra(SearchManager.QUERY, "\"" + num + "\"");
		si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(si);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		/*
		if (BlockedNumberContract.canCurrentUserBlockNumbers(this)) {
			ContentValues values = new ContentValues();
			values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, "1234567890");
			Uri uri = getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
		}
		*/
		Log.d(TAG, intent.toString());
		String act = intent.getAction();
		if (act.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			String num = session.getLastIncomingNumber();
			if (num == null) {
				if (session.getRejectPrivateNumCalls() && rejectCall()) {
					//TODO: notifica chiamata rifiutata
					//TODO: COSA SUCCEDE A UN'EVENTUALE CONVERSAZIONE IN CORSO (iniziata prima)???
					Toast.makeText(getApplicationContext(), R.string.endcall_private_toast, Toast.LENGTH_LONG).show();
				}
			} else if (!contactExists(num)) {
				shakede.start(this);
			}
		} else if (act.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			shakede.stop();
			session.restoreRingVolume();
			// niente?
		} else if (act.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			shakede.stop();
			session.restoreRingVolume();
			String inum = session.getLastIdleNumber();
			if (inum != null) {
				String snum = session.getLastSearchedNumber();
				if (snum != null && snum.equals(inum)) {
					//TODO: notifica chiamata terminata (numero non in rubrica)
				}
			}
		} else if (act.equals(ACT_OPEN_LOG)) {
			Intent li = new Intent();
			li.setAction(Intent.ACTION_VIEW);
			li.setType(CallLog.Calls.CONTENT_TYPE);
			li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(li);
		} else if (act.equals(ACT_TEST_START)) {
			session.setLastIncomingNumber("+393472002591");
			shakede.start(this);
		} else if (act.equals(ACT_TEST_STOP)) {
			//session.setLastCallNumber(null);
			shakede.stop();
		}
	}

	private boolean rejectCall() {
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
			Log.e(TAG, "rejectCall", err);
			return false;
		}
		Log.d(TAG, "Call ended");
		return true;
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
			Log.e(TAG, "contactExists", err);
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
}
