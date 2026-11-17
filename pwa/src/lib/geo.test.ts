import { describe, expect, it } from 'vitest';
import { calculateDistanceMeters, formatDistance } from './geo';

describe('calculateDistanceMeters', () => {
  it('returns 0 for identical coordinates', () => {
    expect(calculateDistanceMeters(48.8566, 2.3522, 48.8566, 2.3522)).toBe(0);
  });

  it('is symmetric', () => {
    const a = calculateDistanceMeters(48.8566, 2.3522, 48.8606, 2.3376);
    const b = calculateDistanceMeters(48.8606, 2.3376, 48.8566, 2.3522);
    expect(a).toBe(b);
  });

  it('computes a plausible Paris intra-muros distance (~1.1 km)', () => {
    // Paris -> Louvre area, roughly 1.1 km apart
    const d = calculateDistanceMeters(48.8566, 2.3522, 48.8606, 2.3376);
    expect(d).toBeGreaterThan(900);
    expect(d).toBeLessThan(1400);
  });
});

describe('formatDistance', () => {
  it('uses metres below 1 km', () => {
    expect(formatDistance(850)).toBe('850 m');
  });

  it('uses kilometres from 1 km onward', () => {
    expect(formatDistance(1500)).toBe('1.5 km');
  });
});
