import { Page } from 'puppeteer';

export class LoginPage {
    private page: Page;

    private readonly loginFormSelector = 'form.login-form';
    private readonly usernameInputSelector = 'input#username';
    private readonly passwordInputSelector = 'input#password';
    private readonly submitButtonSelector = 'button[type="submit"]';

    constructor(page: Page) {
        this.page = page;
    }

    async waitForLoginForm(): Promise<void> {
        await this.page.waitForSelector(this.loginFormSelector, { timeout: 10000 });
    }

    async login(username: string, password: string): Promise<void> {
        await this.waitForLoginForm();
        await this.page.type(this.usernameInputSelector, username);
        await this.page.type(this.passwordInputSelector, password);
        await this.page.click(this.submitButtonSelector);
    }

}