import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import './Header.css';

const Header = () => {
  const { user, logout, isAuthenticated } = useAuth();

  const handleLogout = () => {
    if (window.confirm('Are you sure you want to logout?')) {
      logout();
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-left">
          <h1 className="app-title">🧠 Quiz Generator</h1>
        </div>
        
        <div className="header-right">
          <div className="user-info">
            <span className="user-greeting">
              Welcome, <strong>{user?.username}</strong>
            </span>
            {user?.role && (
              <span className={`user-role ${user.role.toLowerCase()}`}>
                {user.role}
              </span>
            )}
          </div>
          
          <button 
            onClick={handleLogout}
            className="logout-button"
            title="Logout"
          >
            <span className="logout-icon">🚪</span>
            Logout
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
