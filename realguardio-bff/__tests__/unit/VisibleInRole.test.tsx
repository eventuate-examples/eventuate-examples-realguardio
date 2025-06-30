import { render, screen } from '@testing-library/react';
import { useSession } from 'next-auth/react';
import VisibleInRole from '../../components/VisibleInRole';

// Mock next-auth/react module
jest.mock('next-auth/react');

// Type the mocked useSession
const mockUseSession = useSession as jest.Mock;

describe('VisibleInRole', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders children when user has the required role', () => {
    mockUseSession.mockReturnValue({
      data: {
        authorities: ['ADMIN', 'USER']
      },
      status: 'authenticated'
    });

    render(
      <VisibleInRole requiredRole="ADMIN">
        <div>Admin Content</div>
      </VisibleInRole>
    );

    expect(screen.getByText('Admin Content')).toBeInTheDocument();
  });

  it('does not render children when user does not have the required role', () => {
    mockUseSession.mockReturnValue({
      data: {
        authorities: ['USER']
      },
      status: 'authenticated'
    });

    render(
      <VisibleInRole requiredRole="ADMIN">
        <div>Admin Content</div>
      </VisibleInRole>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });

  it('does not render children when there is no session', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated'
    });

    render(
      <VisibleInRole requiredRole="ADMIN">
        <div>Admin Content</div>
      </VisibleInRole>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });

  it('does not render children when session has no authorities', () => {
    mockUseSession.mockReturnValue({
      data: {
        user: { name: 'Test User' }
        // authorities is missing
      },
      status: 'authenticated'
    });

    render(
      <VisibleInRole requiredRole="ADMIN">
        <div>Admin Content</div>
      </VisibleInRole>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });

  it('renders children when user has one of multiple roles', () => {
    mockUseSession.mockReturnValue({
      data: {
        authorities: ['USER', 'MANAGER']
      },
      status: 'authenticated'
    });

    render(
      <VisibleInRole requiredRole="MANAGER">
        <div>Manager Content</div>
      </VisibleInRole>
    );

    expect(screen.getByText('Manager Content')).toBeInTheDocument();
  });
});