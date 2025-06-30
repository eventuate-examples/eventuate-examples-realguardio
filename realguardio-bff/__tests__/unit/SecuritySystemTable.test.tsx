import { render, screen, waitFor } from '@testing-library/react';
import SecuritySystemTable from '../../components/SecuritySystemTable';
import {sample_getSecuritySystems} from '@/types/securitySystemSamples';

// Mock fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('SecuritySystemTable', () => {
  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();
  });

  it('renders securitySystems table with data', async () => {
    // Mock successful securitySystems API response
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => (sample_getSecuritySystems)
    });

    render(<SecuritySystemTable />);

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

    render(<SecuritySystemTable />);

    // Wait for error message
    await waitFor(() => {
      expect(screen.getByText('Error loading securitySystems')).toBeInTheDocument();
    });
  });


  it('handles non-ok API response', async () => {
    // Mock non-ok API response
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error'
    });

    render(<SecuritySystemTable />);

    // Wait for error message
    await waitFor(() => {
      expect(screen.getByText('Error loading securitySystems')).toBeInTheDocument();
    });
  });
});