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
      <div className="content-section">
        {status === 'authenticated' ? (
          <HomePageSignedIn />
        ) : (
          <HomePageNotSignedIn />
        )}
      </div>

      {/* Add error message container for E2E tests */}
      <div className="error-message" style={{ display: 'none' }}></div>

      <style jsx="true">{`
        .main-content {
          min-height: calc(100vh - 80px);
          background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
        }
        
        .content-section {
          padding: 2rem;
          max-width: 1200px;
          margin: 0 auto;
        }
        
        @media (max-width: 768px) {
          .content-section {
            padding: 1rem;
          }
        }
      `}</style>
    </main>
  );
};

export default Home;
