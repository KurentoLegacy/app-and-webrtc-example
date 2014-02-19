package com.kurento.apps.android.webrtc.example;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import android.content.Context;

public class WebRtcSession {

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcSession.class.getSimpleName());

	private static final String DEFAULT_STUN_ADDRESS = "stun.l.google.com";
	private static final int DEFAULT_STUN_PORT = 19302;
	private static final String DEFAULT_STUN_PASSWORD = "";

	private static boolean initiated = false;

	private PeerConnectionFactory peerConnectionFactory;
	private PeerConnection peerConnection;
	private VideoCapturer capturer;
	private VideoSource videoSource;
	private MediaStream localStream;

	static synchronized void initWebRtc(Context context) {
		if (initiated)
			return;
		PeerConnectionFactory.initializeAndroidGlobals(context.getApplicationContext());
		initiated = true;
	}
	
	WebRtcSession(Context context) {
		initWebRtc (context);
	}

	public synchronized void start() {
		peerConnectionFactory = new PeerConnectionFactory();

		StringBuilder stunAddress = new StringBuilder();
		stunAddress.append("stun:").append(DEFAULT_STUN_ADDRESS).append(":")
				.append(DEFAULT_STUN_PORT);

		log.debug("stun server: " + stunAddress.toString());
		PeerConnection.IceServer stunServer = new PeerConnection.IceServer(
				stunAddress.toString(), "", DEFAULT_STUN_PASSWORD);

		List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
		iceServers.add(stunServer);

		MediaConstraints constraints = new MediaConstraints();
		constraints.optional.add(new KeyValuePair("DtlsSrtpKeyAgreement",
				"true"));

		peerConnection = peerConnectionFactory.createPeerConnection(iceServers,
				constraints, new PeerConnectionObserver());

		localStream = peerConnectionFactory
				.createLocalMediaStream("MediaStream0");

		AudioSource audioSource = peerConnectionFactory
				.createAudioSource(new MediaConstraints());
		AudioTrack audioTrack = peerConnectionFactory.createAudioTrack(
				"AudioTrack0", audioSource);
		localStream.addTrack(audioTrack);

		capturer = VideoCapturer
				.create("Camera 0, Facing back, Orientation 90");

		if (capturer != null) {
			MediaConstraints vc = new MediaConstraints();
			videoSource = peerConnectionFactory.createVideoSource(capturer, vc);
			VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(
					"VideoTrack0", videoSource);
			localStream.addTrack(videoTrack);
		}
		peerConnection.addStream(localStream, new MediaConstraints());
	}

	public synchronized void finish() {
		if (peerConnection != null) {
			peerConnection.close();
			peerConnection.dispose();
			peerConnection = null;
			localStream = null;
		}

		if (capturer != null) {
			capturer.dispose();
			capturer = null;
		}

		if (videoSource != null) {
			videoSource.stop();
			videoSource.dispose();
			videoSource = null;
		}

		if (peerConnectionFactory != null) {
			peerConnectionFactory.dispose();
			peerConnectionFactory = null;
		}
	}

	private class PeerConnectionObserver implements PeerConnection.Observer {
		@Override
		public void onSignalingChange(SignalingState newState) {
			log.debug("peerConnection onSignalingChange: " + newState);
		}

		@Override
		public void onRenegotiationNeeded() {
			log.debug("peerConnection onRenegotiationNeeded");
		}

		@Override
		public void onRemoveStream(MediaStream arg0) {
			log.debug("peerConnection onRemoveStream");
		}

		@Override
		public void onIceGatheringChange(IceGatheringState newState) {
			log.debug("peerConnection onIceGatheringChange: " + newState);
		}

		@Override
		public void onIceConnectionChange(IceConnectionState newState) {
			log.debug("peerConnection onIceConnectionChange: " + newState);
		}

		@Override
		public void onIceCandidate(IceCandidate candidate) {
			log.debug("peerConnection onIceCandidate: " + candidate.sdp);
		}

		@Override
		public void onError() {
			log.debug("peerConnection onError");
		}

		@Override
		public void onDataChannel(DataChannel arg0) {
			log.debug("peerConnection onDataChannel");
		}

		@Override
		public void onAddStream(MediaStream stream) {
			log.debug("peerConnection onAddStream");
		}
	}

}
