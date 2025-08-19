import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import config from '../config/config';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Check for stored credentials on app load
  useEffect(() => {
    const storedCredentials = localStorage.getItem('quiz_auth_credentials');
    if (storedCredentials) {
      try {
        const { username, password } = JSON.parse(storedCredentials);
        if (username && password) {
          setUser({ username });
          setIsAuthenticated(true);
          // Set axios default header for all future requests
          setAuthHeader(username, password);
        }
      } catch (error) {
        console.error('Error parsing stored credentials:', error);
        localStorage.removeItem('quiz_auth_credentials');
      }
    }
    setIsLoading(false);
  }, []);

  const setAuthHeader = (username, password) => {
    const basicAuth = 'Basic ' + btoa(`${username}:${password}`);
    axios.defaults.headers.common['Authorization'] = basicAuth;
  };

  const login = async (username, password) => {
    try {
      setIsLoading(true);
      
      // Test the credentials by making a request to a protected endpoint
      const basicAuth = 'Basic ' + btoa(`${username}:${password}`);
      
      const response = await axios.get(`${config.api.baseURL}/api/auth/me`, {
        headers: {
          'Authorization': basicAuth
        }
      });

      // If successful, store credentials and set user
      const credentials = { username, password };
      localStorage.setItem('quiz_auth_credentials', JSON.stringify(credentials));
      
      setUser({ 
        username: response.data.username || username,
        email: response.data.email || '',
        role: response.data.role || 'USER'
      });
      setIsAuthenticated(true);
      setAuthHeader(username, password);
      
      return { success: true };
      
    } catch (error) {
      console.error('Login failed:', error);
      
      // Clear any stored credentials
      localStorage.removeItem('quiz_auth_credentials');
      delete axios.defaults.headers.common['Authorization'];
      
      if (error.response?.status === 401) {
        return { success: false, error: 'Invalid username or password' };
      } else if (error.code === 'ERR_NETWORK') {
        return { success: false, error: 'Cannot connect to server. Please check if the backend is running.' };
      } else {
        return { success: false, error: 'Login failed. Please try again.' };
      }
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (username, email, password) => {
    try {
      setIsLoading(true);
      
      await axios.post(`${config.api.baseURL}/api/auth/register`, {
        username,
        email,
        password
      });

      // After successful registration, automatically log in
      const loginResult = await login(username, password);
      return loginResult;
      
    } catch (error) {
      console.error('Registration failed:', error);
      
      if (error.response?.status === 400) {
        const errorMessage = error.response.data?.message || 'Registration failed';
        return { success: false, error: errorMessage };
      } else if (error.code === 'ERR_NETWORK') {
        return { success: false, error: 'Cannot connect to server. Please check if the backend is running.' };
      } else {
        return { success: false, error: 'Registration failed. Please try again.' };
      }
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    // Clear stored credentials
    localStorage.removeItem('quiz_auth_credentials');
    
    // Clear axios header
    delete axios.defaults.headers.common['Authorization'];
    
    // Clear user state
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
