'use client';

import React from 'react';
import { useSession, signOut } from 'next-auth/react';
import SecuritySystemTable from './SecuritySystemTable';

const HomePageSignedIn: React.FC = () => {
  const { data: session } = useSession();

    return (
    <div className="container">
      <div className="header">
        <p id="signin-status">Signed in as {session?.user?.name}</p>
        <br/>
        <button
          onClick={() => signOut()}
          className="button"
        >
          Sign out
        </button>
      </div>

      <SecuritySystemTable />

      <style jsx>{`
        .container {
          padding: 2rem;
        }
        .header {
          margin-bottom: 2rem;
        }
        .button {
          padding: 0.5rem 1rem;
          margin: 1rem 0;
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

export default HomePageSignedIn;
