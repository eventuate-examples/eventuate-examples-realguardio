import { render, screen, waitFor } from '@testing-library/react';
import { useSession } from 'next-auth/react';
import Home from '../../app/page';
import {sample_getSecuritySystems} from '@/types/securitySystemSamples';

// Mock next-auth
jest.mock('next-auth/react');

// Mock fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('Home', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Mock session by default
    (useSession as jest.Mock).mockReturnValue({
      data: {
        user: { name: 'Test User' },
        authorities: ['ROLE_USER']
      },
      status: 'authenticated'
    });
  });

  it('renders signed-in state with securitySystems table', async () => {
    // Mock successful securitySystems API response
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => (sample_getSecuritySystems)
    });

    render(<Home />);

    // Check loading state
    expect(screen.getByText('Loading securitySystems...')).toBeInTheDocument();

    // Wait for securitySystems to load
    await waitFor(() => {
      expect(screen.queryByText('Loading securitySystems...')).not.toBeInTheDocument();
    });

    // Check if securitySystems table is rendered
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('State')).toBeInTheDocument();

    // Check if securitySystems are displayed
    expect(screen.getByText('ARMED')).toBeInTheDocument();

    expect(screen.getByText('DISARMED')).toBeInTheDocument();

    expect(screen.getByText('ALARMED')).toBeInTheDocument();
  });

  it('handles API error', async () => {
    // Mock failed securitySystems API response
    mockFetch.mockRejectedValueOnce(new Error('Failed to fetch'));

    render(<Home />);

    // Wait for error message
    await waitFor(() => {
      expect(screen.getByText('Error loading securitySystems')).toBeInTheDocument();
    });
  });

  it('renders sign-in button when not authenticated', () => {
    // Mock unauthenticated session
    (useSession as jest.Mock).mockReturnValue({
      data: null,
      status: 'unauthenticated'
    });

    render(<Home />);

    expect(screen.getByText('Please sign in to access your dashboard')).toBeInTheDocument();
  });
});
