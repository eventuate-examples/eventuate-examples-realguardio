'use client';

import React from 'react';
import { useSession } from 'next-auth/react';
import HomePageSignedIn from '../components/HomePageSignedIn';
import HomePageNotSignedIn from '../components/HomePageNotSignedIn';

const Home: React.FC = () => {
  const { data: session, status } = useSession();

  console.log('client session=', session);

  if (status === 'loading') {
    return (
      <div>
        <h1>Loading...</h1>
      </div>
    );
  }

  return (
    <main className="main-content">
      <div className="hero-section">
        <h1 id="welcome-greeting" className="hero-title">Welcome to RealGuardIO!</h1>
        <p className="hero-subtitle">Guarding your real estate since 2025</p>
      </div>

      <div className="content-section">
        {status === 'authenticated' ? (
          <HomePageSignedIn />
        ) : (
          <HomePageNotSignedIn />
        )}
      </div>

      {/* Add error message container for E2E tests */}
      <div className="error-message" style={{ display: 'none' }}></div>

      <style jsx>{`
        .main-content {
          min-height: calc(100vh - 80px);
          background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
        }
        
        .hero-section {
          text-align: center;
          padding: 4rem 2rem 2rem;
          background: rgba(255, 255, 255, 0.1);
          backdrop-filter: blur(10px);
        }
        
        .hero-title {
          font-size: 3.5rem;
          font-weight: 800;
          margin: 0 0 1rem 0;
          background: linear-gradient(45deg, #667eea, #764ba2);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
        }
        
        .hero-subtitle {
          font-size: 1.2rem;
          color: #666;
          margin: 0;
          font-weight: 300;
        }
        
        .content-section {
          padding: 2rem;
          max-width: 1200px;
          margin: 0 auto;
        }
        
        @media (max-width: 768px) {
          .hero-title {
            font-size: 2.5rem;
          }
          
          .hero-section {
            padding: 2rem 1rem 1rem;
          }
          
          .content-section {
            padding: 1rem;
          }
        }
      `}</style>
    </main>
  );
};

export default Home;
