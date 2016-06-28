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
		String state = extras.getString(TelephonyManager.EXTRA_STATE);
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			Intent si = new Intent(context, UService.class);
			if (!extras.containsKey(TelephonyManager.EXTRA_INCOMING_NUMBER))
				si.setAction(UService.ACT_INC_PRIVATE);
			else {
				si.setAction(UService.ACT_INC_WITHNUM);
				si.putExtra(UService.PRM_RCVD_NUMBER, extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER));
			}
			context.startService(si);
		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			Intent si = new Intent(context, UService.class);
			si.setAction(UService.ACT_STATUS_IDLE);
			context.startService(si);
		}
	}
}
