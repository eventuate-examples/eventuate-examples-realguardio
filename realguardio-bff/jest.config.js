module.exports = {
  projects: [
    {
      displayName: 'unit',
      testEnvironment: 'jsdom',
      testMatch: ['<rootDir>/__tests__/unit/**/*.test.{js,jsx,ts,tsx}'],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
      moduleNameMapper: {
        '^@/(.*)$': '<rootDir>/$1',
      },
      transform: {
        '^.+\\.(t|j)sx?$': '@swc/jest',
      },
      transformIgnorePatterns: [
        'node_modules/(?!(next-auth|@babel/runtime|jose|styled-jsx|@panva/hkdf|uuid|preact|preact-render-to-string)/)'
      ],
    },
    {
      displayName: 'e2e',
      testEnvironment: 'node',
      testMatch: ['<rootDir>/__tests__/e2e/**/*.test.{js,jsx,ts,tsx}'],
      transform: {
        '^.+\\.(t|j)sx?$': '@swc/jest',
      },
      transformIgnorePatterns: [
        'node_modules/(?!(next-auth|@babel/runtime|jose|styled-jsx|@panva/hkdf|uuid|preact|preact-render-to-string)/)'
      ],
      setupFilesAfterEnv: ['<rootDir>/jest.puppeteer-setup.js'],
    },
  ],
}; 
