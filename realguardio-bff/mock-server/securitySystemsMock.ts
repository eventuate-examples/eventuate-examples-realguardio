/**
 * Mock server implementation for the SecuritySystems API
 * This server simulates the behavior of the real SecuritySystems service during E2E tests.
 * It implements the endpoints according to the OpenAPI specification and returns sample data.
 */

import express from 'express';
import cors from 'cors';
import { Server } from 'http';
import {sample_getSecuritySystems} from '../types/securitySystemSamples';

const app = express();
app.use(cors());
const port = 3001;


app.get('/securitysystems', (req, res) => {
  console.log("mock server headers", req.headers);

  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or invalid token' });
  }

  res.json(sample_getSecuritySystems);
});

export const startMockServer = async (): Promise<Server> => {
  if (process.env.DISABLE_MOCK_BACKEND) {
    return Promise.resolve({
      close: (fn : (err?: Error) => void) => {
        fn();
      }
    });
  }
  return new Promise((resolve, reject) => {
    const server = app.listen(port, () => {
      console.log(`Mock server is running on http://localhost:${port}`);
      resolve(server);
    });

    server.on('error', (err) => {
      console.log(`Error starting mock server: ${err}`);
      console.error(`Server error: ${err}`);
      reject(err);
    });
  });
};
