package net.apps.ggelardi.uisdis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class UReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras.getString(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			Intent si = new Intent(context, UService.class);
			if (!extras.containsKey(TelephonyManager.EXTRA_INCOMING_NUMBER))
				si.setAction(UService.ACT_NO_NUMBER);
			else {
				si.setAction(UService.ACT_WITH_NUMBER);
				si.putExtra(UService.PRM_INCOMING_NUMBER, extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER));
			}
			context.startService(si);
		}
	}
}
