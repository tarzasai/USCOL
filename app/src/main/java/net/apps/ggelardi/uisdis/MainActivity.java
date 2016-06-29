package net.apps.ggelardi.uisdis;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {
	private static final int PERM_REQUEST = 123;

	private USession session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
			}
		});

		session = USession.getInstance(this);

		List<String> mp = session.missingPermissions();
		if (!mp.isEmpty()) {
			String[] pl = new String[mp.size()];
			pl = mp.toArray(pl);
			ActivityCompat.requestPermissions(this, pl, PERM_REQUEST);
		}

		/*
		Intent si = new Intent(this, UService.class);
		si.setAction(UService.ACT_BEGIN_TEST);
		startService(si);
		*/
	}

	/*
	@Override
	public void onBackPressed() {
		Intent si = new Intent(this, UService.class);
		si.setAction(UService.ACT_END_TEST);
		startService(si);
	}
	*/

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
		if (id == R.id.action_test_search) {
			Intent si = new Intent(this, UService.class);
			si.setAction(UService.ACT_REQ_SEARCH);
			si.putExtra(UService.PRM_RCVD_NUMBER, "3472002591");
			startService(si);
			return true;
		} else if (id == R.id.action_test_shake1) {
			return true;
		} else if (id == R.id.action_test_shake2) {
			return true;
		} else if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		//session.checkNetwork();
	}
}
