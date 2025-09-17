import React, { createContext, useContext, useState, useEffect } from 'react';
import Cookies from 'js-cookie';

interface User {
  id: string;
  email: string;
  name: string;
  verified: boolean;
  createdAt: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<any>;
  register: (name: string, email: string, password: string) => Promise<any>;
  verifyOTP: (email: string, code: string) => Promise<any>;
  resendOTP: (email: string) => Promise<any>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

const API_BASE_URL = 'http://localhost:8080/api';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('auth_token');
    if (savedToken) {
      setToken(savedToken);
      fetchCurrentUser(savedToken);
    } else {
      setLoading(false);
    }
  }, []);

  const fetchCurrentUser = async (authToken: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/users/me`, {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const result = await response.json();
        setUser(result.data);
      } else {
        localStorage.removeItem('auth_token');
        setToken(null);
      }
    } catch (error) {
      console.error('Error fetching user:', error);
      localStorage.removeItem('auth_token');
      setToken(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (email: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    const result = await response.json();
    
    if (response.ok) {
      const authToken = result.data.token;
      localStorage.setItem('auth_token', authToken);
      setToken(authToken);
      await fetchCurrentUser(authToken);
    }

    return result;
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name, email, password }),
    });

    return await response.json();
  };

  const verifyOTP = async (email: string, code: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/verify-otp`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, code }),
    });

    return await response.json();
  };

  const resendOTP = async (email: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/resend-otp`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    return await response.json();
  };

  const logout = () => {
    localStorage.removeItem('auth_token');
    setToken(null);
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    token,
    login,
    register,
    verifyOTP,
    resendOTP,
    logout,
    loading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};