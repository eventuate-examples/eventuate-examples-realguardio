import { startMockServer } from './securitySystemsMock';

(async () => {
  try {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const server = await startMockServer();
    console.log('Mock server started successfully');
  } catch (error) {
    console.error('Failed to start mock server:', error);
  }
})();
