import React, { useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Row, Col, Card } from 'antd';
import VideoPlayer from '../components/VideoPlayer';
import TrafficStats from '../components/TrafficStats';
import FrameScrubber from '../components/FrameScrubber';
import ViolationList from '../components/ViolationList';
import useAnalysisData from '../hooks/useAnalysisData';

// Simple Loading Spinner Component
const LoadingSpinner = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flexDirection: 'column', padding: '2rem' }}>
    <div className="spinner"></div>
    <p style={{ marginTop: '1rem', fontSize: '1.1em', color: 'var(--text-secondary)' }}>Processing...</p>
  </div>
);


function AnalysisPage() {
  const { videoId } = useParams();
  const { videoStatus, analysisResults, processedVideoUrl, error } = useAnalysisData(videoId);
  const [seekFrame, setSeekFrame] = useState<number | null>(null);
  const [currentVideoFrame, setCurrentVideoFrame] = useState<number>(0);
  const [totalVideoFrames, setTotalVideoFrames] = useState<number>(0);

  const handleSeekVideo = useCallback((frameNumber: number) => {
    setSeekFrame(frameNumber);
  }, []);

  const handleFrameChange = useCallback((frame: number) => {
    setSeekFrame(frame);
  }, []);

  const handleVideoFrameUpdate = useCallback((frame: number) => {
    setCurrentVideoFrame(frame);
  }, []);

  const handleVideoLoaded = useCallback((frames: number) => {
    setTotalVideoFrames(frames);
  }, []);


  if (error) {
    return <div className="analysis-page"><h2>Error: {error}</h2></div>;
  }

  if (videoStatus === 'LOADING' || videoStatus === 'UPLOADED' || videoStatus === 'PROCESSING') {
    return (
      <div className="analysis-page processing-status">
        <h2>Video Analysis Status (ID: {videoId})</h2>
        <LoadingSpinner />
        <p>Current Status: <strong>{videoStatus}</strong></p>
        <p>Please wait, analysis is in progress. This page will update automatically.</p>
        <p>This may take several minutes depending on video length and server load.</p>
      </div>
    );
  }

  if (videoStatus === 'COMPLETED' && analysisResults) {
    return (
      <div className="analysis-page" style={{ padding: '20px' }}>
        <h2>Video Analysis Results (ID: {videoId})</h2>
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Card title="Video Player" bordered={false}>
              {processedVideoUrl ? (
                <VideoPlayer
                  videoUrl={processedVideoUrl}
                  analysisResults={analysisResults}
                  seekFrame={seekFrame}
                  onSeeked={() => setSeekFrame(null)}
                  onFrameUpdate={handleVideoFrameUpdate}
                  onVideoLoaded={handleVideoLoaded}
                />
              ) : (
                <p>Processed video not available.</p>
              )}
            </Card>
          </Col>
          <Col span={24}>
            <Card title="Frame Scrubber" bordered={false}>
              <FrameScrubber
                currentFrame={currentVideoFrame}
                totalFrames={totalVideoFrames}
                onFrameChange={handleFrameChange}
              />
            </Card>
          </Col>
          <Col span={24}>
            <Card title="Traffic Statistics" bordered={false}>
              <TrafficStats stats={analysisResults} />
            </Card>
          </Col>
          <Col span={24}>
            <Card title="Violation Log" bordered={false}>
              <ViolationList violations={analysisResults.trafficEvents as any || []} />
            </Card>
          </Col>
        </Row>
      </div>
    );
  }

  return (
    <div className="analysis-page">
      <h2>Analysis for Video (ID: {videoId})</h2>
      <p>No analysis data available yet or an unexpected status occurred.</p>
    </div>
  );
}

export default AnalysisPage;
