import '@testing-library/jest-dom';

// Mock crypto for PKCE
const mockRandomValues = new Uint8Array([
  1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
  17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
]);

global.crypto = {
  getRandomValues: jest.fn(() => mockRandomValues),
  subtle: {
    digest: jest.fn(() => Promise.resolve(new ArrayBuffer(32)))
  }
};

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: jest.fn(() => ({ data: null, status: 'unauthenticated' })),
  signIn: jest.fn(),
  signOut: jest.fn(),
}));

// Mock jose for JWT operations
jest.mock('jose', () => ({
  importJWK: jest.fn(),
  jwtVerify: jest.fn(),
  base64url: {
    encode: jest.fn(str => Buffer.from(str).toString('base64url')),
    decode: jest.fn(str => Buffer.from(str, 'base64url'))
  }
}));

// Mock styled-jsx
jest.mock('styled-jsx/style', () => ({
  __esModule: true,
  default: () => null
}));

// Mock jsx attribute handling
const originalError = console.error;
console.error = jest.fn((...args) => {
  if (args[0].includes('Received `true` for a non-boolean attribute `jsx`')) {
    return;
  }
  originalError(...args);
});

// Mock environment variables
process.env = {
  ...process.env,
  NEXTAUTH_URL: 'http://localhost:3000',
  NEXTAUTH_SECRET: 'test_secret',
  OAUTH_CLIENT_ID: 'dummy_client_id',
  OAUTH_CLIENT_SECRET: 'dummy_client_secret',
  OAUTH_AUTHORIZATION_URL: 'http://localhost:9000/oauth/authorize',
  OAUTH_TOKEN_URL: 'http://localhost:9000/oauth/token',
  OAUTH_USERINFO_URL: 'http://localhost:9000/userinfo',
};
