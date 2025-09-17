import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';

const API_BASE_URL = 'http://localhost:8080/api';

interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
}

export const useApi = () => {
  const { token } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const makeRequest = async <T = any>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(token && { Authorization: `Bearer ${token}` }),
          ...options.headers,
        },
      });

      const result = await response.json();
      
      if (!response.ok) {
        throw new Error(result.message || 'API request failed');
      }

      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error occurred';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const get = <T = any>(endpoint: string) => makeRequest<T>(endpoint);
  
  const post = <T = any>(endpoint: string, data: any) => 
    makeRequest<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });

  const put = <T = any>(endpoint: string, data: any) => 
    makeRequest<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });

  const del = <T = any>(endpoint: string) => 
    makeRequest<T>(endpoint, { method: 'DELETE' });

  return { get, post, put, delete: del, loading, error };
};