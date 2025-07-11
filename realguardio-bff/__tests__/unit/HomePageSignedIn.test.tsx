import { render } from '@testing-library/react';
import { useSession } from 'next-auth/react';
import HomePageSignedIn from '../../components/HomePageSignedIn';

jest.mock('next-auth/react');
jest.mock('../../components/SecuritySystemTable', () => {
  return function MockSecuritySystemTable() {
    return <div data-testid="security-systems-table">Security Systems</div>;
  };
});

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;

describe('HomePageSignedIn', () => {
  it('renders SecuritySystemTable', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' } },
      status: 'authenticated'
    });

    const { getByTestId } = render(<HomePageSignedIn />);
    
    expect(getByTestId('security-systems-table')).toBeInTheDocument();
  });
});