'use client';

import React from 'react';
import { useSession, signIn, signOut } from 'next-auth/react';

const Header: React.FC = () => {
  const { data: session, status } = useSession();

  return (
    <header className="header">
      <div className="header-content">
        <div className="logo">
          <h1>RealGuardIO</h1>
        </div>
        <div className="auth-section">
          {status === 'loading' ? (
            <div className="loading">Loading...</div>
          ) : status === 'authenticated' ? (
            <div className="signed-in">
              <span className="user-name">Welcome, {session?.user?.name}</span>
              <button
                onClick={() => signOut()}
                className="auth-button sign-out"
              >
                Sign Out
              </button>
            </div>
          ) : (
            <div className="not-signed-in">
              <button
                onClick={() => signIn('oauth2-pkce')}
                className="auth-button sign-in"
                data-provider="oauth2-pkce"
              >
                Login
              </button>
            </div>
          )}
        </div>
      </div>

      <style jsx="true">{`
        .header {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
          padding: 1rem 2rem;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
          position: sticky;
          top: 0;
          z-index: 1000;
        }
        
        .header-content {
          display: flex;
          justify-content: space-between;
          align-items: center;
          max-width: 1200px;
          margin: 0 auto;
        }
        
        .logo h1 {
          margin: 0;
          font-size: 1.8rem;
          font-weight: 700;
          background: linear-gradient(45deg, #fff, #f0f0f0);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }
        
        .auth-section {
          display: flex;
          align-items: center;
          gap: 1rem;
        }
        
        .signed-in {
          display: flex;
          align-items: center;
          gap: 1rem;
        }
        
        .user-name {
          font-size: 0.9rem;
          opacity: 0.9;
        }
        
        .auth-button {
          padding: 0.5rem 1.5rem;
          border: none;
          border-radius: 25px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.3s ease;
          font-size: 0.9rem;
        }
        
        .sign-in {
          background: rgba(255, 255, 255, 0.2);
          color: white;
          border: 2px solid rgba(255, 255, 255, 0.3);
        }
        
        .sign-in:hover {
          background: rgba(255, 255, 255, 0.3);
          transform: translateY(-2px);
        }
        
        .sign-out {
          background: rgba(255, 255, 255, 0.1);
          color: white;
          border: 1px solid rgba(255, 255, 255, 0.2);
        }
        
        .sign-out:hover {
          background: rgba(255, 255, 255, 0.2);
          transform: translateY(-2px);
        }
        
        .loading {
          font-size: 0.9rem;
          opacity: 0.7;
        }
        
        @media (max-width: 768px) {
          .header {
            padding: 1rem;
          }
          
          .header-content {
            flex-direction: column;
            gap: 1rem;
          }
          
          .logo h1 {
            font-size: 1.5rem;
          }
          
          .user-name {
            display: none;
          }
        }
      `}</style>
    </header>
  );
};

export default Header;