import { render } from '@testing-library/react';
import RootLayout from '../../app/layout';

jest.mock('../../app/providers', () => {
  return function MockProviders({ children }: { children: React.ReactNode }) {
    return <div data-testid="providers">{children}</div>;
  };
});

jest.mock('../../components/Header', () => {
  return function MockHeader() {
    return <div data-testid="header">Header</div>;
  };
});

jest.mock('../../app/globals.css', () => ({}));

jest.mock('next/font/google', () => ({
  Geist: () => ({ variable: '--font-geist-sans' }),
  Geist_Mono: () => ({ variable: '--font-geist-mono' }),
}));

describe('RootLayout', () => {
  it('renders HTML structure with providers, header and children', () => {
    const { getByTestId, getByText } = render(
      <RootLayout>
        <div>Test Content</div>
      </RootLayout>
    );
    
    expect(getByTestId('providers')).toBeInTheDocument();
    expect(getByTestId('header')).toBeInTheDocument();
    expect(getByText('Test Content')).toBeInTheDocument();
  });
});