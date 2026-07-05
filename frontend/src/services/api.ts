import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to all requests if present
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (username: string, email: string, password: string) =>
    api.post('/api/auth/register', { username, email, password }),
    
  login: (username: string, password: string) =>
    api.post('/api/auth/login', { username, password }),
    
  getMe: () => api.get('/api/auth/me'),
};

export const roomApi = {
  createRoom: (name: string, language: string) =>
    api.post('/api/rooms', { name, language }),
    
  joinRoom: (code: string) =>
    api.get(`/api/rooms/${code}`),
    
  getMyRooms: () =>
    api.get('/api/rooms/my-rooms'),
    
  saveSnapshot: (code: string, content: string, language: string) =>
    api.post(`/api/rooms/${code}/snapshots`, { content, language }),
    
  getSnapshots: (code: string) =>
    api.get(`/api/rooms/${code}/snapshots`),
    
  updatePermission: (code: string, username: string, role: 'EDITOR' | 'VIEWER') =>
    api.post(`/api/rooms/${code}/permissions`, { username, role }),
};

export const executionApi = {
  executeCode: (sourceCode: string, language: string, input: string) =>
    api.post('/api/execute', { sourceCode, language, input }),
};

export default api;
export { API_BASE_URL };
