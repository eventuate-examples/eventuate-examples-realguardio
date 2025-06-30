'use client';

import React from 'react';
import {signIn} from 'next-auth/react';

const HomePageNotSignedIn: React.FC = () => {
    return (
        <div className="container">
            <div className="header">
                <p id="signin-status">Not signed in</p>
                <br/>
                <button
                    onClick={() => signIn('oauth2-pkce')}
                    className="button"
                    data-provider="oauth2-pkce"
                >Login
                </button>
            </div>

            <style jsx>{`
                .container {
                    padding: 2rem;
                }

                .header {
                    margin-bottom: 2rem;
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

export default HomePageNotSignedIn;