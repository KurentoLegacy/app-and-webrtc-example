/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.apps.android.webrtc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.kurento.android.sdk.mscontrol.webrtc.WebRtcSession;

public class MainActivity extends Activity {

	private static final Logger log = LoggerFactory
			.getLogger(MainActivity.class.getSimpleName());

	private WebRtcSession sessionA, sessionB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.main);

		WebRtcSession.initWebRtc(this);

		sessionA = new WebRtcSession();
		sessionA.start();

		sessionB = new WebRtcSession();
		sessionB.start();

		sessionA.createSdpOffer(new WebRtcSession.Callback<String>() {

			@Override
			public void onSuccess(String result) {
				log.debug("offer: " + result);
				sessionB.createSdpAnswer(result,
						new WebRtcSession.Callback<String>() {
							@Override
							public void onSuccess(String result) {
								log.debug("answer: " + result);
								sessionA.processSdpAnswer(result,
										new WebRtcSession.Callback<Void>() {

											@Override
											public void onSuccess(Void result) {
												log.debug("processSdpAnswer onSuccess");
												final LinearLayout localViewA = (LinearLayout) findViewById(R.id.video_capture_surface_container_A);
												final LinearLayout remoteViewA = (LinearLayout) findViewById(R.id.video_receive_surface_container_A);
												final LinearLayout localViewB = (LinearLayout) findViewById(R.id.video_capture_surface_container_B);
												final LinearLayout remoteViewB = (LinearLayout) findViewById(R.id.video_receive_surface_container_B);

												runOnUiThread(new Runnable() {
													public void run() {
														sessionA.setRemoteDisplay(remoteViewA);
														sessionA.setLocalDisplay(localViewA);
														sessionB.setRemoteDisplay(remoteViewB);
														sessionB.setLocalDisplay(localViewB);
													}
												});
											}

											@Override
											public void onError(Exception e) {
												log.error(
														"processSdpAnswer error",
														e);
											}
										});
							}

							@Override
							public void onError(Exception e) {
								log.error("createSdpAnswer error", e);
							}
						});
			}

			@Override
			public void onError(Exception e) {
				log.error("generateSdpOffer error", e);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		sessionA.finish();
		sessionB.finish();
	}

}
