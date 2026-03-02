import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import NotFoundPage from './NotFoundPage';

describe('NotFoundPage', () => {
  it('displays the 404 heading', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    );
    expect(screen.getByText('404')).toBeInTheDocument();
  });

  it('displays a descriptive message', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    );
    expect(screen.getByText('Page not found')).toBeInTheDocument();
    expect(
      screen.getByText("The page you're looking for doesn't exist or has been moved."),
    ).toBeInTheDocument();
  });

  it('renders a link back to the home page', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    );
    const link = screen.getByRole('link', { name: 'Back to Home' });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/');
  });
});
