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
    <div className="container">
      <h1 id="welcome-greeting" style={{ fontSize: '3rem', fontWeight: 'bold', margin: '2rem 0' }}>Welcome to RealguardIO!</h1>

       <p>Guarding your real estate since 2025</p>

      {status === 'authenticated' ? (
        <HomePageSignedIn />
      ) : (
        <HomePageNotSignedIn />
      )}

      {/* Add error message container for E2E tests */}
      <div className="error-message" style={{ display: 'none' }}></div>

      <style jsx>{`
        .container {
          padding: 2rem;
          text-align: center;
        }
        .button {
          padding: 0.5rem 1rem;
          margin: 1rem;
          background-color: #0070f3;
          color: white;
          bsecuritySystem: none;
          bsecuritySystem-radius: 4px;
          cursor: pointer;
        }
        .button:hover {
          background-color: #0051cc;
        }
      `}</style>
    </div>
  );
};

export default Home;
