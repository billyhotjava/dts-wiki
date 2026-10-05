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

// Layout APIs are supplied by real browsers; component tests do not measure geometry.
if (globalThis.ResizeObserver === undefined) {
  (globalThis as unknown as { ResizeObserver: unknown }).ResizeObserver = NoopIntersectionObserver;
}
if (window.matchMedia === undefined) {
  window.matchMedia = query => ({ matches: false, media: query, onchange: null,
    addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {}, dispatchEvent: () => false });
}
