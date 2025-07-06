'use client';

import {GetSecuritySystemResponse, GetSecuritySystemsResponse, SecuritySystemAction} from "@/types/api"

import React, { useEffect, useState } from 'react';


const SecuritySystemTable: React.FC = () => {
  const [securitySystems, setSecuritySystems] = useState<GetSecuritySystemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const handleActionClick = (securitySystemId: number, action: SecuritySystemAction) => {
    console.log(`Performing action ${action} on security system ${securitySystemId}`);
    // Here we would make an API call to trigger the action
    // For now, just log the action
  };

  useEffect(() => {
    const fetchSecuritySystems = async () => {
      try {
        const response = await fetch('/api/securitysystems');
        if (!response.ok) {
          throw new Error('Failed to fetch securitySystems');
        }
        const data: GetSecuritySystemsResponse = await response.json();
        setSecuritySystems(data.securitySystems);
        setError(null);
      } catch (err) {
        setError('Error loading securitySystems');
        console.error('Error:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchSecuritySystems();
  }, []);

  return (
    <div className="securitySystems-section">
      <h2>SecuritySystems</h2>
      {loading && <p>Loading securitySystems...</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && (
        <table className="securitySystems-table">
          <thead>
            <tr>
              <th>Location name</th>
              <th>State</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {securitySystems.map((securitySystem) => (
              <tr key={securitySystem.id}>
                <td>{securitySystem.locationName}</td>
                <td>{securitySystem.state}</td>
                <td>
                  {securitySystem.actions.map((action) => (
                    <button
                      key={action}
                      onClick={() => handleActionClick(securitySystem.id, action)}
                      className="action-button"
                    >
                      {action}
                    </button>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <style jsx="true">{`
        .securitySystems-section {
          margin-top: 2rem;
        }
        .securitySystems-table {
          width: 100%;
          bsecuritySystem-collapse: collapse;
          margin-top: 1rem;
        }
        .securitySystems-table th,
        .securitySystems-table td {
          padding: 0.75rem;
          text-align: left;
          bsecuritySystem: 1px solid #ddd;
        }
        .securitySystems-table th {
          background-color: #f4f4f4;
          font-weight: bold;
        }
        .securitySystems-table tr:nth-child(even) {
          background-color: #f8f8f8;
        }
        .error {
          color: red;
          margin-top: 1rem;
        }
        .action-button {
          margin-right: 0.5rem;
          padding: 0.25rem 0.5rem;
          background-color: #4CAF50;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 0.9rem;
        }
        .action-button:hover {
          background-color: #45a049;
        }
      `}</style>
    </div>
  );
};

export default SecuritySystemTable;
