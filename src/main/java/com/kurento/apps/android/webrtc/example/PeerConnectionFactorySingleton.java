package com.kurento.apps.android.webrtc.example;

import org.webrtc.PeerConnectionFactory;

import android.content.Context;

public class PeerConnectionFactorySingleton extends PeerConnectionFactory {

	private static boolean initiated = false;
	private static PeerConnectionFactorySingleton instance = null;

	private PeerConnectionFactorySingleton() {
	}

	public static synchronized void initWebRtc(Context context) {
		if (initiated)
			return;
		PeerConnectionFactory.initializeAndroidGlobals(context
				.getApplicationContext());
		initiated = true;
	}

	public synchronized static PeerConnectionFactorySingleton getInstance() {
		if (instance == null) {
			instance = new PeerConnectionFactorySingleton();
		}

		return instance;
	}

}
