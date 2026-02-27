import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Spinner from './Spinner';

describe('Spinner', () => {
  it('renders an svg element', () => {
    const { container } = render(<Spinner />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('applies the default className when none is provided', () => {
    const { container } = render(<Spinner />);
    const svg = container.querySelector('svg')!;
    expect(svg.className.baseVal).toContain('h-6');
    expect(svg.className.baseVal).toContain('w-6');
  });

  it('applies a custom className', () => {
    const { container } = render(<Spinner className="h-10 w-10" />);
    const svg = container.querySelector('svg')!;
    expect(svg.className.baseVal).toContain('h-10');
    expect(svg.className.baseVal).toContain('w-10');
  });

  it('has the animate-spin class for animation', () => {
    const { container } = render(<Spinner />);
    const svg = container.querySelector('svg')!;
    expect(svg.className.baseVal).toContain('animate-spin');
  });

  it('contains a circle and a path element', () => {
    const { container } = render(<Spinner />);
    expect(container.querySelector('circle')).toBeInTheDocument();
    expect(container.querySelector('path')).toBeInTheDocument();
  });
});
