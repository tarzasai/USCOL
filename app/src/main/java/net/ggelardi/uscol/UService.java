package net.ggelardi.uscol;

import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

public class UService extends IntentService implements ShakeDetector.Listener {
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
		String num = session.getLastIncomingNumber(null);
		if (num == null)
			return;
		// unlock device if necessary
		KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
		if (km.inKeyguardRestrictedInputMode()) {
			KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock(TAG);
			keyguardLock.disableKeyguard();
		}
		// check network preferences
		if (!onWIFI() && session.getSearchOnWiFiOnly()) {
			Toast.makeText(getApplicationContext(), R.string.toast_noroam, Toast.LENGTH_LONG).show();
			return;
		}
		session.setLastSearchedNumber(num);
		String qry = "\"" + num + "\"";
		// try to clean the number
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber pn = pu.parse(num, "");
			if (pn.hasNationalNumber()) {
				if (pn.hasCountryCode())
					pn.clearCountryCode();
				num = String.valueOf(pn.getNationalNumber());
				if (pn.hasItalianLeadingZero())
					num = StringUtils.repeat("0", pn.getNumberOfLeadingZeros()) + num;
				qry += " OR \"" + num + "\"";
			}
		} catch (NumberParseException e) {
			Log.e(TAG, "hearShake", e);
			qry += " OR \"" + PhoneNumberUtil.normalizeDigitsOnly(num) + "\"";
		}
		// launch web search
		Intent si = new Intent(Intent.ACTION_WEB_SEARCH);
		si.putExtra(SearchManager.QUERY, qry);
		si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(si);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, intent.toString());
		String act = intent.getAction();
		if (act.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			String num = session.getLastIncomingNumber(null);
			if (num == null) {
				if (session.getRejectPrivateNums() && rejectCall()) {
					//TODO: COSA SUCCEDE A UN'EVENTUALE CONVERSAZIONE IN CORSO (iniziata prima)???
					Toast.makeText(getApplicationContext(), R.string.toast_nopriv, Toast.LENGTH_LONG).show();
				}
			} else if (!contactExists(num))
				shakede.start(this);
		} else if (act.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			shakede.stop();
			session.restoreRingVolume();
			// LastAnswered è già stato impostato, qui non c'è altro da fare.
		} else if (act.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			shakede.stop();
			session.restoreRingVolume();
			String inum = session.getLastIdleNumber("aaa");
			String snum = session.getLastSearchedNumber("bbb");
			if (snum.equals(inum))
				sendNotification(snum);
		} else if (act.equals("USERVICE_TEST_SHAKE")) {
			session.setLastIncomingNumber(getString(R.string.action_test_sample));
			shakede.start(this);
		} else if (act.equals("USERVICE_TEST_NOTIF")) {
			sendNotification("3472002591");
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
		Log.d(TAG, "Looking for a contact with number " + phoneNumber);
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

	private void sendNotification(String num) {
		Intent ri = new Intent();
		ri.setAction(Intent.ACTION_VIEW);
		ri.setType(CallLog.Calls.CONTENT_TYPE);
		ri.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
		nb.setSmallIcon(R.drawable.ic_stat_call_info);
		nb.setContentTitle(String.format(getString(R.string.notif_title), num));
		nb.setContentText(getString(R.string.notif_text));
		nb.setContentIntent(pi);

		Notification notif = nb.build();
		notif.flags |= Notification.FLAG_AUTO_CANCEL;

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.notify(1023, notif);
	}
}
