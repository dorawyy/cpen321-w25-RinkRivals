import {
  describe,
  expect,
  test,
  beforeAll,
  afterEach,
  jest,
} from '@jest/globals';
import { NHLService, GameStatus } from '../../src/services/nhl.service';
import axios from 'axios';

/**
 * UNMOCKED Integration Tests for NHLService.getGameStatus
 *
 * These tests make REAL API calls to the NHL API
 * They verify actual integration with external services
 *
 * WARNING: These tests are dependent on:
 * - NHL API availability
 * - Network connectivity
 * - Real game data existing
 *
 * Tests may be slow and can fail if:
 * - NHL API is down
 * - Network is unavailable
 * - Game IDs used no longer exist
 */

describe('Unmocked NHLService.getGameStatus (Integration)', () => {
  let nhlService: NHLService;

  beforeAll(() => {
    nhlService = new NHLService();
  });

  afterEach(() => {
    // Clear cache after each test to ensure fresh API calls
    nhlService.clearCache();
  });

  // Input: Real NHL game ID from 2024 season
  // Expected behavior: Fetches real game data from NHL API
  // Expected output: GameStatus object with valid game state
  // Note: This test uses a real game ID - may fail if game doesn't exist
  test('Fetches real game status from NHL API', async () => {
    // Using a game ID from early 2024 season (should be finished)
    const realGameId = '2024020001';

    const result = await nhlService.getGameStatus(realGameId);

    // The game should exist (even if we don't know its exact state)
    // If result is null, the game might not exist or API might be down
    if (result === null) {
      console.warn(
        `Game ${realGameId} not found - API might be down or game ID invalid`
      );
    }

    // If we get a result, verify its structure
    if (result !== null) {
      expect(result).toHaveProperty('gameId');
      expect(result).toHaveProperty('gameState');
      expect(result).toHaveProperty('gameScheduleState');
      expect(result).toHaveProperty('startTimeUTC');
      expect(result).toHaveProperty('isLive');
      expect(result).toHaveProperty('isFinished');
      expect(result).toHaveProperty('isScheduled');

      // Verify boolean flags are mutually exclusive
      const stateCount = [
        result.isLive,
        result.isFinished,
        result.isScheduled,
      ].filter(state => state).length;
      expect(stateCount).toBeLessThanOrEqual(1);

      // Verify game state is a valid string
      expect(typeof result.gameState).toBe('string');
      expect(result.gameState.length).toBeGreaterThan(0);
    }
  }, 15000); // 15 second timeout for API call

  // Input: Invalid/non-existent game ID
  // Expected behavior: Handles gracefully
  // Expected output: null
  test('Handles non-existent game ID gracefully', async () => {
    const invalidGameId = '9999999999';

    const result = await nhlService.getGameStatus(invalidGameId);

    // Should return null for non-existent game
    expect(result).toBeNull();
  }, 15000);

  // Input: Same game ID called twice
  // Expected behavior: First call hits API, second uses cache
  // Expected output: Same result both times
  test('Caching works with real API calls', async () => {
    const gameId = '2024020001';

    // First call - hits API
    const result1 = await nhlService.getGameStatus(gameId);

    // Second call - should use cache (within 30 second TTL)
    const startTime = Date.now();
    const result2 = await nhlService.getGameStatus(gameId);
    const duration = Date.now() - startTime;

    // Second call should be much faster (cached)
    expect(duration).toBeLessThan(100); // Should be nearly instant

    // Results should be the same
    if (result1 !== null && result2 !== null) {
      expect(result2.gameId).toBe(result1.gameId);
      expect(result2.gameState).toBe(result1.gameState);
    }
  }, 15000);

  // Input: Clear cache and verify next call hits API again
  // Expected behavior: Cache cleared, fresh API call made (timing-based verification)
  // Expected output: Valid GameStatus with slower response time (indicating real API call)
  test('Cache clearing works correctly with real API', async () => {
    const gameId = '2024020001';

    // First call - measure time (real API call)
    const start1 = Date.now();
    const result1 = await nhlService.getGameStatus(gameId);
    const duration1 = Date.now() - start1;

    // Clear cache
    nhlService.clearCache(gameId);

    // Next call should hit API again (not cached) - measure time
    const start2 = Date.now();
    const result2 = await nhlService.getGameStatus(gameId);
    const duration2 = Date.now() - start2;

    // Both calls should take real API time (not instant like cache would be)
    // This verifies cache was actually cleared and API was called again
    if (result1 !== null && result2 !== null) {
      expect(result1).toHaveProperty('gameId', gameId);
      expect(result2).toHaveProperty('gameId', gameId);
      // Both should take some time (real API calls, not cached)
      expect(duration1).toBeGreaterThan(50);
      expect(duration2).toBeGreaterThan(50);
    }
  }, 15000);

  // Input: Multiple different game IDs
  // Expected behavior: Each game cached separately
  // Expected output: Different GameStatus objects
  test('Handles multiple games independently', async () => {
    const gameId1 = '2024020001';
    const gameId2 = '2024020002';

    const result1 = await nhlService.getGameStatus(gameId1);
    const result2 = await nhlService.getGameStatus(gameId2);

    // If both games exist, they should have different IDs
    if (result1 !== null && result2 !== null) {
      expect(result1.gameId).not.toBe(result2.gameId);
    }
  }, 20000);

  // Input: Game status request during off-season or no games
  // Expected behavior: Handles empty schedule gracefully
  // Expected output: null or valid game status
  test('Handles schedule with no current games', async () => {
    // This test verifies the service doesn't crash when schedule is empty
    // Use a very old game ID that definitely won't be in current schedule
    const oldGameId = '2020020001';

    const result = await nhlService.getGameStatus(oldGameId);

    // Should either find it via direct endpoint or return null
    // Either way, shouldn't throw an error
    expect(result === null || typeof result === 'object').toBe(true);
  }, 15000);

  // Input: Verify time until start calculation
  // Expected behavior: Returns correct time difference
  // Expected output: Number (milliseconds)
  test('Calculates time until game start correctly', async () => {
    const gameId = '2024020001';
    const result = await nhlService.getGameStatus(gameId);

    if (result !== null) {
      const timeUntilStart = nhlService.getTimeUntilStart(result.startTimeUTC);

      // Should be a number
      expect(typeof timeUntilStart).toBe('number');

      // For past games, should be negative
      if (result.isFinished) {
        expect(timeUntilStart).toBeLessThan(0);
      }
    }
  }, 15000);

  // Input: Malformed game ID string
  // Expected behavior: Handles gracefully without crashing
  // Expected output: null
  test('Handles malformed game ID formats', async () => {
    const malformedIds = ['abc123', '12-34-56', '', '   ', 'not-a-number'];

    for (const malformedId of malformedIds) {
      const result = await nhlService.getGameStatus(malformedId);
      // Should return null or handle gracefully without throwing
      expect(result === null || typeof result === 'object').toBe(true);
    }
  }, 30000); // Longer timeout for multiple requests
});
