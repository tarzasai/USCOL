package net.apps.ggelardi.uisdis;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
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

	public boolean getOnlySearchOnWiFi() {
		return prefs.getBoolean("only_search_on_wifi", true);
	}

	public boolean getRejectPrivateNumCalls() {
		return prefs.getBoolean("reject_private_numbers", false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		//
	}
}
