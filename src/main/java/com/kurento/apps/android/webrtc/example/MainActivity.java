package com.kurento.apps.android.webrtc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import android.app.Activity;
import android.os.Bundle;

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

		PeerConnectionFactorySingleton.initWebRtc(this);

		AudioSource audioSource = PeerConnectionFactorySingleton.getInstance()
				.createAudioSource(new MediaConstraints());
		createVideoSource();

		sessionA = new WebRtcSession();
		sessionA.start(audioSource, videoSource);

		sessionB = new WebRtcSession();
		sessionB.start(audioSource, videoSource);

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
												log.error("processSdpAnswer onSuccess");
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
		sessionA.finish();
		sessionB.finish();

		disposeVideoSource();

		super.onDestroy();
	}

}
