import { Page } from 'puppeteer';
import {GetSecuritySystemResponse} from "@/types/api";

export class HomePage {
    private page: Page;
    private readonly url = 'http://localhost:3000';
    private readonly apiUrl: string;

    // Selectors
    private readonly welcomeGreetingSelector = '#welcome-greeting';
    private readonly userNameSelector = '.user-name';
    private readonly signInButtonSelector = 'button[data-provider="oauth2-pkce"]';
    private readonly signOutButtonSelector = '.sign-out';
    private readonly securitySystemsTableSelector = '.securitySystems-table';
    private readonly loadingSelector = 'text/Loading securitySystems...';
    private readonly errorSelector = '.error';

    constructor(page: Page, apiUrl: string = 'http://localhost:3001') {
        this.page = page;
        this.apiUrl = apiUrl;
    }

    async navigate(): Promise<void> {
        await this.page.goto(this.url);
        await this.page.waitForSelector(this.welcomeGreetingSelector);
    }

    async getWelcomeText(): Promise<string> {
        const element = await this.page.$(this.welcomeGreetingSelector);
        return element ? (await element.evaluate(el => el.textContent)) || '' : '';
    }

    async getSignInStatus(): Promise<string> {
        try {
            // Wait a moment for the page to stabilize
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Check if signed in (user name is present)
            const userNameElement = await this.page.$(this.userNameSelector);
            if (userNameElement) {
                const userName = await userNameElement.evaluate(el => el.textContent);
                return userName || '';
            }
            
            // Check if sign in button is present (not signed in)
            const signInButton = await this.page.$(this.signInButtonSelector);
            if (signInButton) {
                return 'Not signed in';
            }
            
            // Check if loading state
            const loadingElement = await this.page.$('.loading');
            if (loadingElement) {
                return 'Loading...';
            }
            
            return 'Unknown status';
        } catch (error) {
            console.error('Error getting sign-in status:', error);
            return 'Error checking status';
        }
    }

    async clickSignIn(): Promise<void> {
        await this.page.waitForSelector(this.signInButtonSelector);
        await this.page.click(this.signInButtonSelector);
    }

    async isSignedIn(): Promise<boolean> {
        const status = await this.getSignInStatus();
        return status.includes('Welcome, user1');
    }

    async expectWelcomeTextToBe(expectedText: string): Promise<void> {
        const welcomeText = await this.getWelcomeText();
        expect(welcomeText).toBe(expectedText);
    }

    async expectSignInStatusToBe(expectedStatus: string): Promise<void> {
        const signinStatus = await this.getSignInStatus();
        expect(signinStatus).toBe(expectedStatus);
    }

    async expectToBeSignedIn() : Promise<void> {
        return this.expectSignInStatusToBe('Welcome, user1');
    }

    async waitForSecuritySystemsTable(): Promise<void> {
        await this.page.waitForSelector(this.securitySystemsTableSelector, { timeout: 10000 });
    }


    async getSecuritySystemsTableRows(): Promise<string[][]> {
        await this.waitForSecuritySystemsTable();
        return this.page.evaluate((selector) => {
            const rows = Array.from(document.querySelector(selector)?.querySelectorAll('tbody tr') || []);
            return rows.map(row => {
                const cells = Array.from(row.querySelectorAll('td'));
                return cells.map(cell => cell.textContent || '');
            });
        }, this.securitySystemsTableSelector);
    }

    async waitForLoadingToComplete(): Promise<void> {
        try {
            await this.page.waitForFunction(
                (selector) => !document.querySelector(selector),
                { timeout: 5000 },
                this.loadingSelector
            );
        } catch (_error) {
            throw new Error('Loading indicator did not disappear');
        }
    }

    async expectSecuritySystemInTable(securitySystem: GetSecuritySystemResponse): Promise<void> {
        const rows = await this.getSecuritySystemsTableRows();
        const securitySystemRow = rows.find(row => row[0] == securitySystem.locationName);
        expect(securitySystemRow).toBeTruthy();
        expect(securitySystemRow![1]).toBe(securitySystem.state);
    }
}
