import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { PropertiesForm } from '../src/features/edit/PropertiesForm';
import { splitFrontmatter } from '../src/utils/frontmatter';

vi.mock('../src/api/client', () => ({ api: { get: async () => ({ data: { properties: {} } }) } }));
afterEach(() => cleanup());
it('retains valid frontmatter fences and synchronizes recovered YAML without emitting a publish', async () => {
  const change = vi.fn();
  const view = render(<PropertiesForm initialYaml={'---\ntype: page\n# Preserve comment\ntitle: Original\n---\n'} onChange={change} />);
  fireEvent.click(screen.getByRole('switch'));
  const yaml = screen.getByPlaceholderText('type: page');
  expect(yaml).toHaveValue('type: page\n# Preserve comment\ntitle: Original\n');
  fireEvent.change(yaml, { target: { value: 'type: page\n# Preserve comment\ntitle: Edited\n' } });
  const saved = change.mock.calls[0][0] as string;
  expect(splitFrontmatter(saved + 'Body').front).toBe(saved);
  expect(splitFrontmatter(saved + 'Body').body).toBe('Body');
  view.rerender(<PropertiesForm initialYaml={'---\ntype: page\ntitle: Recovered\n---\n'} onChange={change} />);
  expect(yaml).toHaveValue('type: page\ntitle: Recovered\n');
  expect(change).toHaveBeenCalledTimes(1);
});
