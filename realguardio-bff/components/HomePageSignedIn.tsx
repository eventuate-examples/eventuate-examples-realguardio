'use client';

import React from 'react';
import { useSession, signOut } from 'next-auth/react';
import SecuritySystemTable from './SecuritySystemTable';

const HomePageSignedIn: React.FC = () => {
  const { data: session } = useSession();

    return (
    <div className="container">
      <div className="content">
        <SecuritySystemTable />
      </div>

      <style jsx="true">{`
        .container {
          background: white;
          border-radius: 12px;
          padding: 2rem;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
          margin: 2rem 0;
        }
        
        .content {
          max-width: 100%;
        }
      `}</style>
    </div>
  );
};

export default HomePageSignedIn;
