package net.ggelardi.uscol;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by giorgio on 25/06/2016.
 */
public class USession implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String[] PERMLIST = {
		Manifest.permission.RECEIVE_BOOT_COMPLETED,
		Manifest.permission.ACCESS_NETWORK_STATE,
		Manifest.permission.ACCESS_WIFI_STATE,
		Manifest.permission.CHANGE_WIFI_STATE,
		Manifest.permission.INTERNET,
		Manifest.permission.READ_PHONE_STATE,
		Manifest.permission.CALL_PHONE,
		Manifest.permission.READ_CONTACTS,
		Manifest.permission.SYSTEM_ALERT_WINDOW
	};

	private static Context appContext;
	private static USession singleton;

	private final SharedPreferences prefs;

	public USession(Context context) {
		appContext = context.getApplicationContext();

		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	public static USession getInstance(Context context) {
		if (singleton == null)
			singleton = new USession(context);
		return singleton;
	}

	public List<String> missingPermissions() {
		List<String> perms = new ArrayList<>();
		for (String p : PERMLIST)
			if (ContextCompat.checkSelfPermission(appContext, p) != PackageManager.PERMISSION_GRANTED)
				perms.add(p);
		return perms;
	}

	public SharedPreferences getPrefs() {
		return prefs;
	}

	public void setRejectPrivateNums(boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PK.NOPRIV, value);
		editor.commit();
	}

	public boolean getRejectPrivateNums() {
		return prefs.getBoolean(PK.NOPRIV, false);
	}

	public void setSearchOnWiFiOnly(boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PK.NOROAM, value);
		editor.commit();
	}

	public boolean getSearchOnWiFiOnly() {
		return prefs.getBoolean(PK.NOROAM, true);
	}

	/**/

	public void lowerRingVolume() {
		AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PK.SAVED_RINGVOL, am.getStreamVolume(AudioManager.STREAM_RING));
		editor.commit();
		am.setStreamVolume(AudioManager.STREAM_RING, 1, AudioManager.FLAG_PLAY_SOUND);
	}

	public void restoreRingVolume() {
		AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, prefs.getInt(PK.SAVED_RINGVOL, 3), AudioManager.FLAG_PLAY_SOUND);
	}

	/**/

	private void setPhoneNumber(String prefkey, @Nullable String value) {
		SharedPreferences.Editor editor = prefs.edit();
		if (value != null)
			editor.putString(prefkey, value);
		else
			editor.remove(prefkey);
		editor.commit();
	}

	private String getPhoneNumber(String prefkey) {
		return prefs.getString(prefkey, null);
	}

	/**/

	public void setLastIncomingNumber(@Nullable String value) {
		setPhoneNumber(PK.LAST_INCOMING, value);
	}

	public String getLastIncomingNumber() {
		return getPhoneNumber(PK.LAST_INCOMING);
	}

	/**/

	public void setLastSearchedNumber(@Nullable String value) {
		setPhoneNumber(PK.LAST_SEARCHED, value);
	}

	public String getLastSearchedNumber() {
		return getPhoneNumber(PK.LAST_SEARCHED);
	}

	/**/

	public void setLastAnsweredNumber(@Nullable String value) {
		setPhoneNumber(PK.LAST_ANSWERED, value);
	}

	public String getLastAnsweredNumber() {
		return getPhoneNumber(PK.LAST_ANSWERED);
	}

	/**/

	public void setLastIdleNumber(@Nullable String value) {
		setPhoneNumber(PK.LAST_IDLESTAT, value);
	}

	public String getLastIdleNumber() {
		return getPhoneNumber(PK.LAST_IDLESTAT);
	}

	/**/

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		//
	}

	public static class PK {
		// user preferences
		public static final String NOPRIV = "pk_hang_privates";
		public static final String NOROAM = "pk_wifi_searches";
		// persisted variables (temp)
		public static final String SAVED_RINGVOL = "mem_ring_volume";
		public static final String LASTPN = "mem_last_number";

		public static final String LAST_INCOMING = "mem_last_incoming";
		public static final String LAST_SEARCHED = "mem_last_searched";
		public static final String LAST_ANSWERED = "mem_last_answered";
		public static final String LAST_IDLESTAT = "mem_last_idlestate";
	}
}
