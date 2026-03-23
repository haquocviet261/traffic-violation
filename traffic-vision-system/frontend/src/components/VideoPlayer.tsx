import React, { useRef, useEffect, useState, useCallback } from 'react';

interface RectDto {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface LineDto {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

interface TrafficEvent {
  id: string;
  frameNumber: number;
  timestamp: number;
  eventType: 'RED_LIGHT_VIOLATION' | string;
  bboxX: number;
  bboxY: number;
  bboxWidth: number;
  bboxHeight: number;
  imagePath: string;
}

interface AnalysisResults {
  videoId: string;
  trafficLightRects: RectDto[];
  stopLines: LineDto[];
  trafficEvents: TrafficEvent[];
}

interface VideoPlayerProps {
  videoUrl: string | null;
  analysisResults: AnalysisResults | null;
  seekFrame?: number | null;
  onSeeked?: () => void;
  onFrameUpdate?: (currentFrame: number) => void;
  onVideoLoaded?: (totalFrames: number) => void;
  autoplayOnSeek?: boolean;
}

declare global {
  interface HTMLVideoElement {
    fps: number;
  }
}

const VideoPlayer: React.FC<VideoPlayerProps> = ({
  videoUrl,
  analysisResults,
  seekFrame,
  onSeeked,
  onFrameUpdate,
  onVideoLoaded,
  autoplayOnSeek,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const videoId = analysisResults?.videoId;
  const streamingUrl = videoId ? `http://localhost:8080/api/videos/${videoId}` : undefined;

  const getCurrentFrameNumber = useCallback(() => {
    const video = videoRef.current;
    if (video) {
      return Math.floor(video.currentTime * (video.fps || 30));
    }
    return 0;
  }, []);


  useEffect(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !analysisResults) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;

      const currentFrameNumber = getCurrentFrameNumber();
      if (onFrameUpdate) {
        onFrameUpdate(currentFrameNumber);
      }

      // 2. Draw Vehicle Violations
      if (analysisResults.trafficEvents) {
        const eventsInFrame = analysisResults.trafficEvents.filter(
          (event) => event.frameNumber === currentFrameNumber
        );

        eventsInFrame
          .filter(e => e.eventType === 'RED_LIGHT_VIOLATION')
          .forEach((event) => {
            ctx.beginPath();
            ctx.rect(event.bboxX, event.bboxY, event.bboxWidth, event.bboxHeight);
            ctx.lineWidth = 3;
            ctx.strokeStyle = 'red';
            ctx.stroke();

            ctx.fillStyle = 'red';
            ctx.font = 'bold 14px Arial';
            ctx.fillText('RED LIGHT VIOLATION', event.bboxX, event.bboxY - 5);
          });
      }
    };

    const handleTimeUpdate = () => {
      draw();
    };

    const handleSeeked = () => {
      draw();
      if (onSeeked) {
        onSeeked();
      }
      if (autoplayOnSeek) {
        video.play();
      }
    };


    video.addEventListener('play', draw);
    video.addEventListener('timeupdate', handleTimeUpdate);
    video.addEventListener('seeked', handleSeeked);


    video.addEventListener('loadeddata', () => {
      console.log('VideoPlayer: Data loaded successfully, duration:', video.duration);
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      video.fps = 30; // Initialize fps
      if (onVideoLoaded) {
        onVideoLoaded(Math.floor(video.duration * video.fps));
      }
      draw();
    });

    video.addEventListener('error', (e) => {
      console.error('VideoPlayer: Error event triggered on video element');
      if (video.error) {
        console.error('VideoPlayer: Error Code:', video.error.code);
        console.error('VideoPlayer: Error Message:', video.error.message);
      }
    });

    video.addEventListener('stalled', () => console.warn('VideoPlayer: Network stalled'));
    video.addEventListener('waiting', () => console.log('VideoPlayer: Waiting for data...'));

    return () => {
      video.removeEventListener('play', draw);
      video.removeEventListener('timeupdate', handleTimeUpdate);
      video.removeEventListener('seeked', handleSeeked);
    };
  }, [
    streamingUrl,
    analysisResults,
    onSeeked,
    onFrameUpdate,
    onVideoLoaded,
    autoplayOnSeek,
    getCurrentFrameNumber
  ]);

  useEffect(() => {
    const video = videoRef.current;
    if (video && seekFrame !== null && seekFrame !== undefined && video.fps) {
      video.currentTime = seekFrame / video.fps;
    }
  }, [seekFrame]);

  if (!streamingUrl) {
    return <div className="video-player-container">Preparing video stream...</div>;
  }

  return (
    <div style={{ position: 'relative', width: '100%', maxWidth: '800px', margin: 'auto' }}>
      <video 
        ref={videoRef} 
        controls 
        width="100%"
        onLoadedMetadata={() => console.log('VideoPlayer: Metadata loaded')}
      >
        <source src={streamingUrl} type="video/mp4" />
        Your browser does not support the video tag or the H264 codec.
      </video>
      <canvas
        ref={canvasRef}
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          pointerEvents: 'none'
        }}
      />
    </div>
  );
};

export default VideoPlayer;
