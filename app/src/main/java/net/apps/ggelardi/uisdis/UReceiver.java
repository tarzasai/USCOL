package net.apps.ggelardi.uisdis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class UReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		USession session = USession.getInstance(context);
		session.setLastCallNumber(null);
		Bundle extras = intent.getExtras();
		String state = extras.getString(TelephonyManager.EXTRA_STATE);
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			Intent si = new Intent(context, UService.class);
			si.setAction(UService.ACT_HANDLE_CALL);
			if (extras.containsKey(TelephonyManager.EXTRA_INCOMING_NUMBER))
				session.setLastCallNumber(extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER));
			context.startService(si);
		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			Intent si = new Intent(context, UService.class);
			si.setAction(UService.ACT_STATUS_IDLE);
			context.startService(si);
		}
	}
}
