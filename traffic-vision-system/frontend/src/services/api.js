import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/videos';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Optional: Add a request interceptor
api.interceptors.request.use(
  (config) => {
    // You can add authentication tokens here
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Optional: Add a response interceptor for global error handling
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      // The request was made and the server responded with a status code
      // that falls out of the range of 2xx
      console.error('API Error:', error.response.data);
      console.error('Status:', error.response.status);
      console.error('Headers:', error.response.headers);
      return Promise.reject(new Error(error.response.data.message || `API Error: ${error.response.status}`));
    } else if (error.request) {
      // The request was made but no response was received
      console.error('No response received:', error.request);
      return Promise.reject(new Error('Network Error: No response from server.'));
    } else {
      // Something happened in setting up the request that triggered an Error
      console.error('Error setting up request:', error.message);
      return Promise.reject(new Error(`Request Error: ${error.message}`));
    }
  }
);

export const uploadVideo = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await api.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

export const getVideoStatus = async (videoId) => {
  try {
    const response = await api.get(`/${videoId}/status`);
    return response.data; // Axios wraps data in .data
  } catch (error) {
    throw error;
  }
};

export const getAnalysisResults = async (videoId) => {
  try {
    const response = await api.get(`/${videoId}/analysis`);
    return response.data;
  } catch (error) {
    throw error;
  }
};

export const getProcessedVideo = async (videoId) => {
  try {
    const response = await api.get(`/${videoId}/processed-video`);
    return response.data; // This will be the URL string
  } catch (error) {
    throw error;
  }
};

export const getDashboardStats = async () => {
  try {
    const response = await api.get('/dashboard-stats');
    return response.data;
  } catch (error) {
    throw error;
  }
};
