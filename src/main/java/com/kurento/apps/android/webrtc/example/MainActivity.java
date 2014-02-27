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
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

	private static final Logger log = LoggerFactory
			.getLogger(MainActivity.class.getSimpleName());

	private static VideoCapturer videoCapturer;
	private static VideoSource videoSource;
	private WebRtcSession sessionA, sessionB;

	private static void createVideoSource() {
		PeerConnectionFactory peerConnectionFactory = PeerConnectionFactorySingleton
				.getInstance();

		videoCapturer = VideoCapturer
				.create("Camera 0, Facing back, Orientation 90");
		log.debug("videoCapturer: " + videoCapturer);

		MediaConstraints vc = new MediaConstraints();

		videoSource = peerConnectionFactory
				.createVideoSource(videoCapturer, vc);
	}

	private static void disposeVideoSource() {
		if (videoCapturer != null) {
			videoCapturer.dispose();
			videoCapturer = null;
		}

		if (videoSource != null) {
			videoSource.stop();
			videoSource.dispose();
			videoSource = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.main);

		PeerConnectionFactorySingleton.initWebRtc(this);
		createVideoSource();

		sessionA = new WebRtcSession();
		sessionA.start(videoSource);

		sessionB = new WebRtcSession();
		sessionB.start(videoSource);

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
														sessionA.setRemoteDisplay(remoteViewB);
														sessionA.setLocalDisplay(localViewB);
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

		disposeVideoSource();
	}

}
