// Set default timeout for all tests
jest.setTimeout(30000);

// Set environment variables for E2E tests
process.env = {
  ...process.env,
  NEXTAUTH_URL: 'http://localhost:3000',
  NEXTAUTH_SECRET: 'test_secret',
  OAUTH_CLIENT_ID: 'dummy_client_id',
  OAUTH_CLIENT_SECRET: 'dummy_client_secret',
  OAUTH_AUTHORIZATION_URL: 'http://localhost:9000/oauth/authorize',
  OAUTH_TOKEN_URL: 'http://localhost:9000/oauth/token',
  OAUTH_USERINFO_URL: 'http://localhost:9000/userinfo',
  NODE_ENV: 'test',
};

const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

global.browser = null;
global.page = null;

beforeAll(async () => {
  global.browser = await puppeteer.launch({
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  global.page = await global.browser.newPage();
});

afterAll(async () => {
  await global.browser.close();
});

afterEach(async () => {
  if (expect.getState().currentTestName && expect.getState().currentTestName.includes('should sign in and display signed in status')) {
    const testName = expect.getState().currentTestName.replace(/\s+/g, '_');
    const screenshotPath = path.resolve(__dirname, `screenshots/${testName}.png`);
    const htmlPath = path.resolve(__dirname, `html/${testName}.html`);

    // Create directories if they don't exist
    fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
    fs.mkdirSync(path.dirname(htmlPath), { recursive: true });

    // Take a screenshot
    await global.page.screenshot({ path: screenshotPath });

    // Save HTML source
    const html = await global.page.content();
    fs.writeFileSync(htmlPath, html);
  }
});