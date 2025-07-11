import { render } from '@testing-library/react';
import Providers from '../../app/providers';

jest.mock('next-auth/react', () => ({
  SessionProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="session-provider">{children}</div>
  ),
}));

describe('Providers', () => {
  it('wraps children with SessionProvider', () => {
    const { getByTestId, getByText } = render(
      <Providers>
        <div>Test Content</div>
      </Providers>
    );
    
    expect(getByTestId('session-provider')).toBeInTheDocument();
    expect(getByText('Test Content')).toBeInTheDocument();
  });
});