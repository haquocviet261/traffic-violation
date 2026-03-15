import React from 'react';
import { Slider } from 'antd';

interface FrameScrubberProps {
  currentFrame: number;
  totalFrames: number;
  onFrameChange: (frame: number) => void;
}

const FrameScrubber: React.FC<FrameScrubberProps> = ({ currentFrame, totalFrames, onFrameChange }) => {
  return (
    <div style={{ padding: '20px' }}>
      <Slider
        min={0}
        max={totalFrames > 0 ? totalFrames - 1 : 0} // Ensure max is valid
        onChange={onFrameChange}
        value={currentFrame}
        tooltip={{ formatter: (value) => `Frame: ${value}` }}
        disabled={totalFrames === 0}
      />
      <div style={{ textAlign: 'center', marginTop: '10px' }}>
        Current Frame: {currentFrame} / {totalFrames > 0 ? totalFrames - 1 : 0}
      </div>
    </div>
  );
};

export default FrameScrubber;
