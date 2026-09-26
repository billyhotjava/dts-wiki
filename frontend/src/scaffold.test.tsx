import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

// Placeholder suite: proves `pnpm test` runs (W1). Real suites arrive with W4+
describe('frontend scaffold', () => {
  it('renders a heading', () => {
    render(<h1>DTS Wiki</h1>);
    expect(screen.getByRole('heading', { name: 'DTS Wiki' })).toBeInTheDocument();
  });
});
