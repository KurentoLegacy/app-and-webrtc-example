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
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.Callbacks;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class WebRtcSession {

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcSession.class.getSimpleName());

	private static final String DEFAULT_STUN_ADDRESS = "stun.l.google.com";
	private static final int DEFAULT_STUN_PORT = 19302;
	private static final String DEFAULT_STUN_PASSWORD = "";

	private PeerConnectionFactory peerConnectionFactory;
	private PeerConnection peerConnection;
	private MediaStream localStream;

	public synchronized void start(VideoSource videoSource) {
		peerConnectionFactory = PeerConnectionFactorySingleton.getInstance();

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

		if (videoSource != null) {
			VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(
					"VideoTrack0", videoSource);

			videoTrack.addRenderer(new VideoRenderer(new Callbacks() {

				@Override
				public void setSize(int width, int height) {
					log.debug("(" + WebRtcSession.this + ") setSize " + width
							+ "x" + height);
				}

				@Override
				public void renderFrame(I420Frame frame) {
					log.debug("(" + WebRtcSession.this + ") renderFrame "
							+ frame);
				}
			}));

			localStream.addTrack(videoTrack);
		} else {
			log.warn("Cannot create VideoCapturer");
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
