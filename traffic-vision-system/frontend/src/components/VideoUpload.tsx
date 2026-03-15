import React, { useState } from 'react';

interface VideoUploadProps {
  onFileUpload: (file: File) => void;
}

function VideoUpload({ onFileUpload }: VideoUploadProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      setSelectedFile(event.target.files[0]);
    } else {
      setSelectedFile(null); // Clear selected file if nothing is chosen
    }
  };

  const handleSubmit = () => {
    if (selectedFile) {
      onFileUpload(selectedFile);
    } else {
      alert('Please select a file first!');
    }
  };

  return (
    <div className="video-upload-component">
      <h3>Upload New Video</h3>
      <input type="file" accept="video/*" onChange={handleFileChange} />
      <button onClick={handleSubmit} disabled={!selectedFile}>Upload</button>
    </div>
  );
}

export default VideoUpload;
