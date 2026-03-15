import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadVideo } from '../services/api';

function UploadPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      setSelectedFile(event.target.files[0]);
    } else {
      setSelectedFile(null);
    }
    setMessage('');
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('Please select a file first!');
      return;
    }

    setLoading(true);
    setMessage('Uploading and processing video...');
    try {
      const response = await uploadVideo(selectedFile);

      if (response.videoId) {
        setMessage(`Upload successful! Video ID: ${response.videoId}`);
        navigate(`/analysis/${response.videoId}`);
      } else {
        setMessage(`Upload failed: ${response.message || 'Unknown error'}`);
      }
    } catch (error: unknown) {
      if (error instanceof Error) {
        setMessage(`Error during upload: ${error.message}`);
      } else {
        setMessage('Error during upload: An unknown error occurred.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="upload-page">
      <h2>Upload Traffic Video</h2>
      <input type="file" accept="video/*" onChange={handleFileChange} />
      <button onClick={handleUpload} disabled={!selectedFile || loading}>
        {loading ? 'Uploading...' : 'Upload Video'}
      </button>
      {message && <p className="message">{message}</p>}
    </div>
  );
}

export default UploadPage;
