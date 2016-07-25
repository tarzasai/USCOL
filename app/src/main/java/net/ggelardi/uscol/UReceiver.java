package net.ggelardi.uscol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

public class UReceiver extends BroadcastReceiver {
	private static final String TAG = "UReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		String state = extras.getString(TelephonyManager.EXTRA_STATE);
		if (state == null)
			return;
		String num = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER, "");
		Log.d(TAG, String.format("state: %1$s - num: '%2$s'", state, num));
		USession session = USession.getInstance(context);
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			session.setLastIncomingNumber(num.equals("") ? null : num);
		} else if (num.equals("")) {
			// è OFFHOOK o IDLE ma era un numero privato, quindi non modifico LastAnswered e LastIdle
		} else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			if (num.equals(session.getLastIncomingNumber("xxx")))
				session.setLastAnsweredNumber(num);
		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			if (num.equals(session.getLastIncomingNumber("xxx")))
				session.setLastIdleNumber(num);
		}
		context.startService(new Intent(context, UService.class).setAction(state));
	}
}
