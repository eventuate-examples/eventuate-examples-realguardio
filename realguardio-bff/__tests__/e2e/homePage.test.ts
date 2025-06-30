import { Browser, Page } from 'puppeteer';
import puppeteer from 'puppeteer';
import { HomePage } from './pageObjects/HomePage';
import { LoginPage } from './pageObjects/LoginPage';
import expect from "expect";
import {afterAll, beforeAll, describe, it} from "@jest/globals";
import { startMockServer } from '../../mock-server/securitySystemsMock';
import { Server } from 'http';

import { oaklandSystem, berkeleySystem, haywardSystem } from '../../types/securitySystemSamples';

/**
 * End-to-end tests for the Home Page
 * These tests verify the integration between the frontend and the SecuritySystems API,
 * using a mock server to simulate the API responses.
 */
describe('Home Page', () => {
  let browser: Browser;
  let page: Page;
  let homePage: HomePage;
  let loginPage: LoginPage;
  let mockServer: Server;

  beforeAll(async () => {
    try {

      console.log("starting mock server");
      mockServer = await startMockServer();
      console.log("started mock server");
      process.env.SECURITY_SYSTEMS_API_URL = 'http://localhost:3001';
      browser = await puppeteer.launch({
        args: ['--no-sandbox', '--disable-setuid-sandbox']
      });
      page = await browser.newPage();
      homePage = new HomePage(page, 'http://localhost:3001');
      loginPage = new LoginPage(page);
    } catch (error) {
      console.error('Error in beforeAll:', error);
      // Ensure cleanup if setup fails
      if (mockServer) {
        await new Promise<void>((resolve) => {
          mockServer.close(() => resolve());
        });
      }
      if (browser) {
        await browser.close();
      }
      throw error;
    }
  });

  afterAll(async () => {
    try {
      if (browser) {
        await browser.close();
      }
      if (mockServer) {
        await new Promise<void>((resolve, reject) => {
          mockServer.close((err) => {
            if (err) {
              console.error('Error closing mock server:', err);
              reject(err);
            } else {
              resolve();
            }
          });
        });
      }
    } catch (error) {
      console.error('Error in afterAll:', error);
    } finally {
      delete process.env.SECURITY_SYSTEMS_API_URL;
    }
  });

  it('should start mock server', async() => {
    expect(mockServer).toBeTruthy();
  })

  it('should display the home page', async () => {
    await homePage.navigate();

    await homePage.expectWelcomeTextToBe('Welcome to RealguardIO!');
    await homePage.expectSignInStatusToBe('Not signed in');
  });

  it('should sign in and display signed in status', async () => {
    await homePage.navigate();
    await homePage.clickSignIn();

    await loginPage.login('user1', 'password');

    await homePage.expectToBeSignedIn();
  });

  /**
   * Verifies that the securitySystems table is properly displayed after signing in.
   * Tests:
   * - Table visibility
   * - Correct number of securitySystems from mock data
   * - Table headers match specification
   * - SecuritySystem data matches mock server response
   * - All securitySystem states (APPROVED, REJECTED, PENDING) are properly displayed
   * - Rejection reasons are shown when applicable
   */
  it('should display securitySystems table after signing in', async () => {
    await homePage.expectToBeSignedIn();

    await homePage.waitForSecuritySystemsTable();

    // Get all rows and verify structure
    const rows = await homePage.getSecuritySystemsTableRows();
    expect(rows.length).toBe(3); // Should match the number of sample securitySystems

    // Verify table headers are present
    const headers = await page.$$eval('.securitySystems-table th', ths => ths.map(th => th.textContent));
    expect(headers).toEqual(['Location name',  'State', 'Actions']);

    // Verify specific securitySystems from the mock data
    await homePage.expectSecuritySystemInTable(oaklandSystem);
    await homePage.expectSecuritySystemInTable(berkeleySystem);
    await homePage.expectSecuritySystemInTable(haywardSystem);
  });



});
