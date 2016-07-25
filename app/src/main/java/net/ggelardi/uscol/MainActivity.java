package net.ggelardi.uscol;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {
	private static final int PERM_REQUEST = 123;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		final USession session = USession.getInstance(this);

		// check & ask permissions
		List<String> mp = session.missingPermissions();
		if (!mp.isEmpty()) {
			String[] pl = new String[mp.size()];
			pl = mp.toArray(pl);
			ActivityCompat.requestPermissions(this, pl, PERM_REQUEST);
		}

		CheckBox chkWiFi = (CheckBox) findViewById(R.id.chkWifi);
		chkWiFi.setChecked(session.getSearchOnWiFiOnly());
		chkWiFi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				session.setSearchOnWiFiOnly(b);
			}
		});

		CheckBox chkNoPN = (CheckBox) findViewById(R.id.chkNoPriv);
		chkNoPN.setChecked(session.getRejectPrivateNums());
		chkNoPN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				session.setRejectPrivateNums(b);
			}
		});

		// check beta version
		String versionName = "1.0";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		if (!versionName.endsWith("b")) {
			findViewById(R.id.txtDebug).setVisibility(View.GONE);
			findViewById(R.id.grdDebug).setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.txtDbgInc)).setText(session.getLastIncomingNumber("n/a"));
			((TextView) findViewById(R.id.txtDbgSrc)).setText(session.getLastSearchedNumber("n/a"));
			((TextView) findViewById(R.id.txtDbgAns)).setText(session.getLastAnsweredNumber("n/a"));
			((TextView) findViewById(R.id.txtDbgIdl)).setText(session.getLastIdleNumber("n/a"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		//noinspection SimplifiableIfStatement
		if (id == R.id.action_test_shake) {
			Intent si = new Intent(this, UService.class);
			si.setAction("USERVICE_TEST_SHAKE");
			startService(si);
			return true;
		}
		/*if (id == R.id.action_test_notif) {
			Intent si = new Intent(this, UService.class);
			si.setAction("USERVICE_TEST_NOTIF");
			startService(si);
			return true;
		}*/
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		//session.checkNetwork();
	}
}
