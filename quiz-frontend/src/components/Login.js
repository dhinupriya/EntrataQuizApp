import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import './Login.css';

const Login = () => {
  const { login, register, isLoading } = useAuth();
  const [isRegistering, setIsRegistering] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: ''
  });
  const [errors, setErrors] = useState({});

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    }
    
    if (isRegistering && !formData.email.trim()) {
      newErrors.email = 'Email is required';
    }
    
    if (isRegistering && formData.email && !/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Email is invalid';
    }
    
    if (!formData.password) {
      newErrors.password = 'Password is required';
    }
    
    if (isRegistering && formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    const { username, email, password } = formData;
    
    let result;
    if (isRegistering) {
      result = await register(username, email, password);
    } else {
      result = await login(username, password);
    }
    
    if (!result.success) {
      setErrors({ submit: result.error });
    }
  };

  const toggleMode = () => {
    setIsRegistering(!isRegistering);
    setErrors({});
    setFormData({
      username: '',
      email: '',
      password: ''
    });
  };

  const fillDemoCredentials = (userType) => {
    if (userType === 'admin') {
      setFormData({
        username: 'admin',
        email: '',
        password: 'admin123'
      });
    } else {
      setFormData({
        username: 'user',
        email: '',
        password: 'user123'
      });
    }
    setErrors({});
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h2>{isRegistering ? 'Create Account' : 'Sign In'}</h2>
          <p>
            {isRegistering 
              ? 'Create a new account to access the quiz generator'
              : 'Sign in to access the quiz generator'
            }
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleInputChange}
              className={errors.username ? 'error' : ''}
              placeholder="Enter your username"
              disabled={isLoading}
            />
            {errors.username && <span className="error-message">{errors.username}</span>}
          </div>

          {isRegistering && (
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                className={errors.email ? 'error' : ''}
                placeholder="Enter your email"
                disabled={isLoading}
              />
              {errors.email && <span className="error-message">{errors.email}</span>}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className={errors.password ? 'error' : ''}
              placeholder="Enter your password"
              disabled={isLoading}
            />
            {errors.password && <span className="error-message">{errors.password}</span>}
          </div>

          {errors.submit && (
            <div className="error-message submit-error">
              {errors.submit}
            </div>
          )}

          <button 
            type="submit" 
            className="login-button"
            disabled={isLoading}
          >
            {isLoading ? 'Please wait...' : (isRegistering ? 'Create Account' : 'Sign In')}
          </button>
        </form>

        <div className="login-footer">
          <p>
            {isRegistering ? 'Already have an account?' : "Don't have an account?"}
            <button 
              type="button" 
              onClick={toggleMode}
              className="link-button"
              disabled={isLoading}
            >
              {isRegistering ? 'Sign In' : 'Create Account'}
            </button>
          </p>
        </div>

        {!isRegistering && (
          <div className="demo-credentials">
            <p className="demo-title">Demo Credentials:</p>
            <div className="demo-buttons">
              <button 
                type="button" 
                onClick={() => fillDemoCredentials('user')}
                className="demo-button"
                disabled={isLoading}
              >
                Fill User Demo (user/user123)
              </button>
              <button 
                type="button" 
                onClick={() => fillDemoCredentials('admin')}
                className="demo-button"
                disabled={isLoading}
              >
                Fill Admin Demo (admin/admin123)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Login;
