import '@testing-library/jest-dom/vitest';

// jsdom lacks IntersectionObserver (used by Milkdown code-block views).
// A no-op stub is enough for serializer-focused tests.
class NoopIntersectionObserver {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
}

if (typeof window !== 'undefined' && window.IntersectionObserver === undefined) {
  (window as unknown as { IntersectionObserver: unknown }).IntersectionObserver = NoopIntersectionObserver;
  (globalThis as unknown as { IntersectionObserver: unknown }).IntersectionObserver = NoopIntersectionObserver;
}
