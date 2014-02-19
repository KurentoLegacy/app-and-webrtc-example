package com.kurento.apps.android.webrtc.example;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	private WebRtcSession session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		session = new WebRtcSession(this);
		session.start();
	}

	@Override
	protected void onDestroy() {
		session.finish();
		super.onDestroy();
	}

}
