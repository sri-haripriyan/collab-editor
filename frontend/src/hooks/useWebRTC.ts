import { useEffect, useRef, useState } from 'react';
import { UserPresence, WsMessage } from '@/types';

interface UseWebRTCOptions {
  roomId: string;
  currentUser: { id: number; username: string } | null;
  activeUsers: UserPresence[];
  sendWsMessage: (msg: WsMessage) => void;
}

export const useWebRTC = ({ roomId, currentUser, activeUsers, sendWsMessage }: UseWebRTCOptions) => {
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [peersMuteState, setPeersMuteState] = useState<Record<number, boolean>>({});

  // Maps targetUserId -> RTCPeerConnection
  const peerConnections = useRef<Record<number, RTCPeerConnection>>({});
  // Ref for the local stream to avoid closure issues in event handlers
  const localStreamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    // Sync ref
    localStreamRef.current = localStream;
  }, [localStream]);

  // Initialize local microphone stream
  const startAudio = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
      setLocalStream(stream);
      setIsMuted(false);
      
      // Auto mute by default if user desires, or active state
      stream.getAudioTracks().forEach(t => t.enabled = true);
      
      // Inform existing participants of connection capability
      activeUsers.forEach(user => {
        if (currentUser && user.userId !== currentUser.id) {
          initiatePeerConnection(user.userId, stream, true);
        }
      });
    } catch (err) {
      // Log mic access failures
    }
  };

  const stopAudio = () => {
    if (localStream) {
      localStream.getTracks().forEach(track => track.stop());
      setLocalStream(null);
    }
    
    // Close all Peer connections
    Object.keys(peerConnections.current).forEach(userIdStr => {
      const uId = parseInt(userIdStr);
      closePeerConnection(uId);
    });
  };

  const closePeerConnection = (userId: number) => {
    if (peerConnections.current[userId]) {
      peerConnections.current[userId].close();
      delete peerConnections.current[userId];
    }
    
    // Remove matching remote audio element if exists
    const audioEl = document.getElementById(`audio-peer-${userId}`);
    if (audioEl) {
      audioEl.remove();
    }
  };

  const initiatePeerConnection = async (targetUserId: number, stream: MediaStream, isInitiator: boolean) => {
    if (peerConnections.current[targetUserId]) {
      return peerConnections.current[targetUserId];
    }

    const pc = new RTCPeerConnection({
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
      ]
    });

    peerConnections.current[targetUserId] = pc;

    // Add local stream tracks
    stream.getTracks().forEach(track => {
      pc.addTrack(track, stream);
    });

    // Handle ICE candidates
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        sendWsMessage({
          type: 'SIGNAL',
          roomId,
          payload: {
            targetUserId,
            signal: {
              type: 'candidate',
              candidate: event.candidate
            }
          }
        });
      }
    };

    // Play remote stream
    pc.ontrack = (event) => {
      const remoteStream = event.streams[0];
      let audioEl = document.getElementById(`audio-peer-${targetUserId}`) as HTMLAudioElement;
      
      if (!audioEl) {
        audioEl = document.createElement('audio');
        audioEl.id = `audio-peer-${targetUserId}`;
        audioEl.autoplay = true;
        document.body.appendChild(audioEl);
      }
      
      audioEl.srcObject = remoteStream;
    };

    if (isInitiator) {
      try {
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        sendWsMessage({
          type: 'SIGNAL',
          roomId,
          payload: {
            targetUserId,
            signal: {
              type: 'offer',
              sdp: pc.localDescription
            }
          }
        });
      } catch (err) {
        // Log offer generation error
      }
    }

    return pc;
  };

  const handleSignalingMessage = async (senderId: number, signal: any) => {
    let pc = peerConnections.current[senderId];

    // If connection doesn't exist, we must have local stream before responding
    if (!pc) {
      let stream = localStreamRef.current;
      if (!stream) {
        try {
          stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
          setLocalStream(stream);
          setIsMuted(false);
        } catch (err) {
          return; // Without mic access we ignore call setups
        }
      }
      pc = await initiatePeerConnection(senderId, stream, false);
    }

    try {
      if (signal.type === 'offer') {
        await pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        sendWsMessage({
          type: 'SIGNAL',
          roomId,
          payload: {
            targetUserId: senderId,
            signal: {
              type: 'answer',
              sdp: pc.localDescription
            }
          }
        });
      } else if (signal.type === 'answer') {
        await pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
      } else if (signal.type === 'candidate' && signal.candidate) {
        await pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
      }
    } catch (err) {
      // Log signaling setup errors
    }
  };

  const toggleMute = () => {
    if (localStream) {
      const audioTrack = localStream.getAudioTracks()[0];
      if (audioTrack) {
        const newMuteState = !isMuted;
        audioTrack.enabled = !newMuteState;
        setIsMuted(newMuteState);
        
        // Sync mute status to room
        sendWsMessage({
          type: 'MIC_STATE',
          roomId,
          payload: { isMuted: newMuteState }
        });
      }
    }
  };

  // Clean up connections on unmount
  useEffect(() => {
    return () => {
      stopAudio();
    };
  }, []);

  return {
    localStream,
    isMuted,
    peersMuteState,
    startAudio,
    stopAudio,
    toggleMute,
    handleSignalingMessage,
    closePeerConnection,
    setPeersMuteState
  };
};
