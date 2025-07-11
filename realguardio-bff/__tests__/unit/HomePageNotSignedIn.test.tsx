import { render, screen } from '@testing-library/react';
import HomePageNotSignedIn from '../../components/HomePageNotSignedIn';

describe('HomePageNotSignedIn', () => {
  it('renders welcome message', () => {
    render(<HomePageNotSignedIn />);
    
    expect(screen.getByText('Welcome to RealGuardIO')).toBeInTheDocument();
  });
});