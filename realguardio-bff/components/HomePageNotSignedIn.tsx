'use client';

import React from 'react';
import {signIn} from 'next-auth/react';

const HomePageNotSignedIn: React.FC = () => {
    return (
        <div className="container">
            <div className="welcome-content">
                <h2>Welcome to RealGuardIO</h2>
                <p>Your comprehensive real estate security management platform</p>
                <div className="features">
                    <div className="feature">
                        <h3>🏠 Property Monitoring</h3>
                        <p>24/7 monitoring of your real estate properties</p>
                    </div>
                    <div className="feature">
                        <h3>🔒 Security Systems</h3>
                        <p>Manage all your security systems from one place</p>
                    </div>
                    <div className="feature">
                        <h3>📊 Analytics</h3>
                        <p>Real-time insights and reporting</p>
                    </div>
                </div>
                <p className="signin-prompt">Please sign in to access your dashboard</p>
            </div>

            <style jsx>{`
                .container {
                    background: white;
                    border-radius: 12px;
                    padding: 3rem;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    margin: 2rem 0;
                    text-align: center;
                }

                .welcome-content h2 {
                    color: #333;
                    margin-bottom: 1rem;
                    font-size: 2rem;
                }

                .welcome-content p {
                    color: #666;
                    font-size: 1.1rem;
                    margin-bottom: 2rem;
                }

                .features {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                    gap: 2rem;
                    margin: 3rem 0;
                }

                .feature {
                    padding: 1.5rem;
                    background: #f8f9fa;
                    border-radius: 8px;
                    border: 1px solid #e9ecef;
                }

                .feature h3 {
                    color: #495057;
                    margin-bottom: 0.5rem;
                    font-size: 1.2rem;
                }

                .feature p {
                    color: #6c757d;
                    margin: 0;
                    font-size: 0.95rem;
                }

                .signin-prompt {
                    font-weight: 500;
                    color: #667eea !important;
                    font-size: 1rem !important;
                    margin-top: 2rem !important;
                }
            `}</style>
        </div>
    );
};

export default HomePageNotSignedIn;