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
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.Callbacks;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class WebRtcSession {

	protected interface Callback<T> {
		void onSuccess(T result);

		void onError(Exception e);
	}

	private static final Logger log = LoggerFactory
			.getLogger(WebRtcSession.class.getSimpleName());

	private static final String DEFAULT_STUN_ADDRESS = "stun.l.google.com";
	private static final int DEFAULT_STUN_PORT = 19302;
	private static final String DEFAULT_STUN_PASSWORD = "";

	private PeerConnectionFactory peerConnectionFactory;
	private PeerConnection peerConnection;
	private MediaStream localStream;

	private PeerConnectionObserver peerConnectionObserver = new PeerConnectionObserver();

	private synchronized String getLocalDescription() {
		if (peerConnection == null) {
			return null;
		}

		return peerConnection.getLocalDescription().description;
	}

	public synchronized void start(AudioSource audioSource,
			VideoSource videoSource) {
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
				constraints, peerConnectionObserver);

		localStream = peerConnectionFactory
				.createLocalMediaStream("MediaStream0");

		AudioTrack audioTrack = peerConnectionFactory.createAudioTrack(
				"AudioTrack0", audioSource);
		localStream.addTrack(audioTrack);

		if (videoSource != null) {
			VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(
					"VideoTrack0", videoSource);

			videoTrack.addRenderer(new VideoRenderer(new Callbacks() {

				@Override
				public void setSize(int width, int height) {
					log.debug("(" + WebRtcSession.this
							+ ") localStream setSize " + width + "x" + height);
				}

				@Override
				public void renderFrame(I420Frame frame) {
					log.debug("(" + WebRtcSession.this
							+ ") localStream renderFrame " + frame);
				}
			}));

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
	}

	public synchronized void createSdpOffer(final Callback<String> callback) {
		MediaConstraints constraints = new MediaConstraints();

		constraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveAudio", "true"));
		constraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveVideo", "true"));

		peerConnectionObserver.setSdpCallback(callback);
		peerConnection.createOffer(new SdpObserver() {

			@Override
			public void onCreateFailure(String error) {
				log.error("createOffer onCreateFailure: " + error);
				callback.onError(new Exception(error));
			}

			@Override
			public void onCreateSuccess(SessionDescription sdp) {
				log.debug("createOffer onSuccess");
				peerConnection.setLocalDescription(new SdpObserver() {
					@Override
					public void onCreateFailure(String error) {
						// Nothing to do
					}

					@Override
					public void onCreateSuccess(SessionDescription sdp) {
						// Nothing to do
					}

					@Override
					public void onSetFailure(String error) {
						log.error("setLocalDescription onFailure: " + error);
						callback.onError(new Exception(error));
					}

					@Override
					public void onSetSuccess() {
						log.debug("setLocalDescription onSuccess");
					}
				}, sdp);
			}

			@Override
			public void onSetFailure(String error) {
				// Nothing to do
			}

			@Override
			public void onSetSuccess() {
				// Nothing to do
			}
		}, constraints);
	}

	public synchronized void createSdpAnswer(String sdpOffer,
			final Callback<String> callback) {
		final SessionDescription sdp = new SessionDescription(
				SessionDescription.Type.OFFER, sdpOffer);

		// FIXME
		peerConnectionObserver.setSdpCallback(callback);
		peerConnection.setRemoteDescription(new SdpObserver() {
			@Override
			public void onCreateFailure(String error) {
				// Nothing to do
			}

			@Override
			public void onCreateSuccess(SessionDescription sdp) {
				// Nothing to do
			}

			@Override
			public void onSetFailure(String error) {
				log.error("setRemoteDescription onFailure: " + error);
				callback.onError(new Exception(error));
			}

			@Override
			public void onSetSuccess() {
				log.debug("setRemoteDescription onSuccess");
				MediaConstraints constraints = new MediaConstraints();
				peerConnection.createAnswer(new SdpObserver() {
					@Override
					public void onCreateFailure(String error) {
						log.debug("createAnswer onFailure: " + error);
						callback.onError(new Exception(error));
					}

					@Override
					public void onCreateSuccess(SessionDescription sdp) {
						log.debug("createAnswer onSuccess");
						peerConnection.setLocalDescription(new SdpObserver() {
							@Override
							public void onCreateFailure(String error) {
								// Nothing to do
							}

							@Override
							public void onCreateSuccess(SessionDescription sdp) {
								// Nothing to do
							}

							@Override
							public void onSetFailure(String error) {
								log.debug("setLocalDescription onFailure: "
										+ error);
								callback.onError(new Exception(error));
							}

							@Override
							public void onSetSuccess() {
								log.debug("setLocalDescription onSuccess");
							}
						}, sdp);
					}

					@Override
					public void onSetFailure(String error) {
						// Nothing to do
					}

					@Override
					public void onSetSuccess() {
						// Nothing to do
					}
				}, constraints);
			}
		}, sdp);
	}

	protected void processSdpAnswer(String sdpAnswer,
			final Callback<Void> callback) {
		final SessionDescription sdp = new SessionDescription(
				SessionDescription.Type.ANSWER, sdpAnswer);

		peerConnection.setRemoteDescription(new SdpObserver() {
			@Override
			public void onCreateFailure(String error) {
				// Nothing to do
			}

			@Override
			public void onCreateSuccess(SessionDescription sdp) {
				// Nothing to do
			}

			@Override
			public void onSetFailure(String error) {
				log.error("setRemoteDescription onFailure: " + error);
				callback.onError(new Exception(error));
			}

			@Override
			public void onSetSuccess() {
				log.debug("setRemoteDescription onSuccess");
				callback.onSuccess(null);
			}
		}, sdp);
	}

	private class PeerConnectionObserver implements PeerConnection.Observer {

		private Callback<String> sdpCalback;

		public synchronized Callback<String> getSdpCallback() {
			return sdpCalback;
		}

		public synchronized void setSdpCallback(Callback<String> sdpCalback) {
			this.sdpCalback = sdpCalback;
		}

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
			if (IceGatheringState.COMPLETE.equals(newState)) {
				Callback<String> c = getSdpCallback();
				if (c == null) {
					log.error("There is not callback");
					return;
				}

				String localDescription = getLocalDescription();
				if (localDescription != null) {
					c.onSuccess(localDescription);
				} else {
					String error = "Local SDP is null";
					log.error(error);
					c.onError(new Exception(error));
				}
			}
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

			if (stream != null && stream.videoTracks.size() > 0) {
				stream.videoTracks.get(0).addRenderer(
						new VideoRenderer(new Callbacks() {

							@Override
							public void setSize(int width, int height) {
								log.debug("(" + WebRtcSession.this
										+ ") remoteStream setSize " + width
										+ "x" + height);
							}

							@Override
							public void renderFrame(I420Frame frame) {
								log.debug("(" + WebRtcSession.this
										+ ") remoteStream renderFrame " + frame);
							}
						}));
			}
		}
	}

}
