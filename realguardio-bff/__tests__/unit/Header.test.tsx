import { render, screen } from '@testing-library/react';
import { useSession } from 'next-auth/react';
import Header from '../../components/Header';

jest.mock('next-auth/react');

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;

describe('Header', () => {
  it('renders RealGuardIO title', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated'
    });

    render(<Header />);
    
    expect(screen.getByText('RealGuardIO')).toBeInTheDocument();
  });
});