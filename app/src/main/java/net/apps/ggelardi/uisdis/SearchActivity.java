package net.apps.ggelardi.uisdis;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SearchActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		WebView webView = (WebView) findViewById(R.id.webView);

		FloatingActionButton btn_accept = (FloatingActionButton) findViewById(R.id.btn_accept);
		btn_accept.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//
			}
		});

		FloatingActionButton btn_reject = (FloatingActionButton) findViewById(R.id.btn_reject);
		btn_reject.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SearchActivity.this.hangup();
			}
		});

		//Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();

		String num = getIntent().getStringExtra(UService.PRM_RCVD_NUMBER);
		String qry = String.format("https://www.google.com/search?q=\"%1$s\"", num);
		webView.getSettings().setJavaScriptEnabled(false);
		webView.setWebViewClient(new WebViewClient());
		webView.loadUrl(qry);
	}

	public void accept() {
		//
		finish();
	}

	public void hangup() {
		Intent si = new Intent(this, UService.class);
		si.setAction(UService.ACT_REQ_HANGUP);
		startService(si);
		finish();
	}
}
