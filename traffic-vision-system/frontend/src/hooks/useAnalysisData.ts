import { useState, useEffect } from 'react';
import { getVideoStatus, getAnalysisResults, getProcessedVideo } from '../services/api';

// --- Interfaces for AnalysisResults ---
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
  vehicleCount: number;
  violationCount: number;
  trafficDensity: number;
  analysisTime: string;
  trafficLightDetections: number;
  trafficLightRects: RectDto[];
  stopLines: LineDto[];
  trafficEvents: TrafficEvent[];
}
// --- End Interfaces ---


const useAnalysisData = (videoId: string | undefined) => {
  const [videoStatus, setVideoStatus] = useState<string>('LOADING');
  const [analysisResults, setAnalysisResults] = useState<AnalysisResults | null>(null);
  const [processedVideoUrl, setProcessedVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    let intervalId: NodeJS.Timeout | undefined; // Specify type for intervalId

    const fetchAnalysisData = async () => {
      if (!videoId) return;

      try {
        const statusResponse: string = await getVideoStatus(videoId);
        setVideoStatus(statusResponse);

        if (statusResponse === 'COMPLETED') {
          // Clear polling interval once completed
          if (intervalId) clearInterval(intervalId);

          const resultsResponse: AnalysisResults = await getAnalysisResults(videoId);
          setAnalysisResults(resultsResponse);

          const processedVideoResponse: string = await getProcessedVideo(videoId);
          setProcessedVideoUrl(processedVideoResponse);

        } else if (statusResponse === 'FAILED') {
          if (intervalId) clearInterval(intervalId);
          setError('Video processing failed.');
        } else if (statusResponse === 'PROCESSING' || statusResponse === 'UPLOADED') {
          // Continue polling
          // Set timeout to poll again, only if no interval is currently active
          if (!intervalId) {
            intervalId = setInterval(fetchAnalysisData, 5000); // Poll every 5 seconds
          }
        }
      } catch (err: any) { // Catch error as any for now, can be refined
        if (intervalId) clearInterval(intervalId);
        setError(`Error fetching analysis data: ${err.message}`);
        setVideoStatus('ERROR');
      }
    };

    // Initial fetch
    fetchAnalysisData();

    // Cleanup function
    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [videoId]); // Dependency array: re-run effect if videoId changes

  return { videoStatus, analysisResults, processedVideoUrl, error };
};

export default useAnalysisData;