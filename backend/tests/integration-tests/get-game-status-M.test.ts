import {
  describe,
  expect,
  test,
  jest,
  beforeEach,
  afterEach,
} from '@jest/globals';
import axios from 'axios';

// Mock axios
jest.mock('axios');

import { NHLService } from '../../src/services/nhl.service';

const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('Mocked NHLService.getGameStatus', () => {
  let nhlService: NHLService;

  beforeEach(() => {
    // Create a fresh instance for each test
    nhlService = new NHLService();
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  afterEach(() => {
    // Clear cache after each test
    nhlService.clearCache();
  });

  // Mocked behavior: Successful API response with live game
  // Input: valid game ID
  // Expected behavior: Returns GameStatus with isLive=true
  // Expected output: GameStatus object
  test('Returns live game status successfully', async () => {
    const mockGameId = '2024020100';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020100,
                gameState: 'LIVE',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-10-31T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValueOnce(mockResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(result!.gameState).toBe('LIVE');
    expect(result!.isLive).toBe(true);
    expect(result!.isFinished).toBe(false);
    expect(result!.isScheduled).toBe(false);
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Successful API response with finished game
  // Input: valid game ID
  // Expected behavior: Returns GameStatus with isFinished=true
  // Expected output: GameStatus object
  test('Returns finished game status successfully', async () => {
    const mockGameId = '2024020101';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020101,
                gameState: 'OFF',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-10-31T20:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValueOnce(mockResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameState).toBe('OFF');
    expect(result!.isLive).toBe(false);
    expect(result!.isFinished).toBe(true);
    expect(result!.isScheduled).toBe(false);
  });

  // Mocked behavior: Successful API response with scheduled game
  // Input: valid game ID
  // Expected behavior: Returns GameStatus with isScheduled=true
  // Expected output: GameStatus object
  test('Returns scheduled game status successfully', async () => {
    const mockGameId = '2024020102';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020102,
                gameState: 'FUT',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-11-02T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValueOnce(mockResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameState).toBe('FUT');
    expect(result!.isLive).toBe(false);
    expect(result!.isFinished).toBe(false);
    expect(result!.isScheduled).toBe(true);
  });

  // Mocked behavior: Game data with empty string gameState (uses default)
  // Input: valid game ID with empty gameState
  // Expected behavior: Uses default value 'FUT' for empty gameState
  // Expected output: GameStatus with default gameState='FUT'
  test('Handles game data with empty gameState', async () => {
    const mockGameId = '2024020103';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020103,
                gameState: '', // Empty string - should default to 'FUT'
                gameScheduleState: '', // Empty string - should default to 'OK'
                startTimeUTC: '2024-11-02T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValueOnce(mockResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(result!.gameState).toBe('FUT'); // Default value for empty string
    expect(result!.gameScheduleState).toBe('OK'); // Default value for empty string
    expect(result!.isScheduled).toBe(true); // Now correctly set
    expect(result!.isLive).toBe(false);
    expect(result!.isFinished).toBe(false);
  });

  // Mocked behavior: Game not in schedule, falls back to direct endpoint
  // Input: valid game ID not in current schedule
  // Expected behavior: Calls direct endpoint and returns status
  // Expected output: GameStatus object from direct endpoint
  test('Falls back to direct endpoint when game not in schedule', async () => {
    const mockGameId = '2024020103';

    // First call to schedule endpoint returns no matching game
    const scheduleResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 9999999, // Different game
                gameState: 'LIVE',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-10-31T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    // Second call to direct endpoint succeeds
    const directResponse = {
      data: {
        gameState: 'OFF',
        gameScheduleState: 'OK',
        startTimeUTC: '2024-10-31T22:00:00Z',
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(scheduleResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(result!.gameState).toBe('OFF');
    expect(mockedAxios.get).toHaveBeenCalledTimes(2);
  });

  // Mocked behavior: API returns network timeout error
  // Input: valid game ID
  // Expected behavior: Handles error gracefully
  // Expected output: null
  test('Handles network timeout error', async () => {
    const mockGameId = '2024020104';
    const timeoutError = new Error('timeout of 10000ms exceeded');
    (timeoutError as any).code = 'ECONNABORTED';

    mockedAxios.get.mockRejectedValueOnce(timeoutError);
    jest.spyOn(axios, 'isAxiosError').mockReturnValue(true);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: API returns 404 Not Found
  // Input: invalid/non-existent game ID
  // Expected behavior: Handles error gracefully
  // Expected output: null
  test('Handles 404 Not Found error', async () => {
    const mockGameId = 'invalid-game-id';
    const notFoundError = {
      response: {
        status: 404,
        data: { message: 'Game not found' },
      },
      message: 'Request failed with status code 404',
    };

    mockedAxios.get.mockRejectedValueOnce(notFoundError);
    jest.spyOn(axios, 'isAxiosError').mockReturnValue(true);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: API returns 500 Internal Server Error
  // Input: valid game ID
  // Expected behavior: Handles error gracefully
  // Expected output: null
  test('Handles 500 Internal Server Error', async () => {
    const mockGameId = '2024020105';
    const serverError = {
      response: {
        status: 500,
        data: { error: 'Internal server error' },
      },
      message: 'Request failed with status code 500',
    };

    mockedAxios.get.mockRejectedValueOnce(serverError);
    jest.spyOn(axios, 'isAxiosError').mockReturnValue(true);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: API returns malformed response (missing gameWeek)
  // Input: valid game ID
  // Expected behavior: Falls back to direct endpoint
  // Expected output: GameStatus from direct endpoint or null
  test('Handles malformed response missing gameWeek', async () => {
    const mockGameId = '2024020106';
    const malformedResponse = {
      data: {
        // Missing gameWeek property
        someOtherData: 'test',
      },
    };

    const directResponse = {
      data: {
        gameState: 'FUT',
        gameScheduleState: 'OK',
        startTimeUTC: '2024-11-02T23:00:00Z',
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(malformedResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(mockedAxios.get).toHaveBeenCalledTimes(2);
  });

  // Mocked behavior: API returns gameWeek with days missing games property
  // Input: valid game ID
  // Expected behavior: Skips days without games, falls back to direct endpoint
  // Expected output: GameStatus from direct endpoint
  test('Handles gameWeek with days missing games property', async () => {
    const mockGameId = '2024020106';
    const malformedResponse = {
      data: {
        gameWeek: [
          {
            // First day has no games property
            date: '2024-10-31',
          },
          {
            // Second day also missing games
            date: '2024-11-01',
            someOtherProp: 'test',
          },
        ],
      },
    };

    const directResponse = {
      data: {
        gameState: 'LIVE',
        gameScheduleState: 'OK',
        startTimeUTC: '2024-10-31T23:00:00Z',
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(malformedResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(result!.gameState).toBe('LIVE');
    expect(mockedAxios.get).toHaveBeenCalledTimes(2);
  });

  // Mocked behavior: API returns empty games array
  // Input: valid game ID
  // Expected behavior: Falls back to direct endpoint
  // Expected output: GameStatus from direct endpoint
  test('Handles empty games array in response', async () => {
    const mockGameId = '2024020107';
    const emptyGamesResponse = {
      data: {
        gameWeek: [
          {
            games: [], // Empty array
          },
        ],
      },
    };

    const directResponse = {
      data: {
        gameState: 'LIVE',
        gameScheduleState: 'OK',
        startTimeUTC: '2024-10-31T23:00:00Z',
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(emptyGamesResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameState).toBe('LIVE');
  });

  // Mocked behavior: Both schedule and direct endpoints fail
  // Input: valid game ID
  // Expected behavior: Returns null after both attempts
  // Expected output: null
  test('Returns null when both endpoints fail', async () => {
    const mockGameId = '2024020108';
    const error = new Error('Network error');

    mockedAxios.get.mockRejectedValue(error);
    jest.spyOn(axios, 'isAxiosError').mockReturnValue(true);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1); // Only schedule call, direct endpoint also fails
  });

  // Mocked behavior: Cache hit on second call
  // Input: same game ID called twice within cache TTL
  // Expected behavior: Second call uses cache, no API call
  // Expected output: Same GameStatus object
  test('Uses cache on subsequent calls within TTL', async () => {
    const mockGameId = '2024020109';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020109,
                gameState: 'LIVE',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-10-31T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValueOnce(mockResponse);

    // First call
    const result1 = await nhlService.getGameStatus(mockGameId);
    expect(result1).not.toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);

    // Second call (should use cache)
    const result2 = await nhlService.getGameStatus(mockGameId);
    expect(result2).not.toBeNull();
    expect(result2).toEqual(result1);
    expect(mockedAxios.get).toHaveBeenCalledTimes(1); // Still only 1 API call
  });

  // Mocked behavior: Direct endpoint returns no data
  // Input: valid game ID
  // Expected behavior: Returns null
  // Expected output: null
  test('Handles direct endpoint returning no data', async () => {
    const mockGameId = '2024020110';

    const scheduleResponse = {
      data: {
        gameWeek: [{ games: [] }],
      },
    };

    const directResponse = {
      data: null, // No data
    };

    mockedAxios.get
      .mockResolvedValueOnce(scheduleResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).toBeNull();
  });

  // Mocked behavior: Direct endpoint returns data with missing optional fields
  // Input: valid game ID, direct endpoint returns minimal data
  // Expected behavior: Uses default values for missing fields
  // Expected output: GameStatus with defaults applied
  test('Handles direct endpoint with missing optional fields', async () => {
    const mockGameId = '2024020112';

    const scheduleResponse = {
      data: {
        gameWeek: [{ games: [] }],
      },
    };

    const directResponse = {
      data: {
        // Missing gameState - should default to 'FUT'
        // Missing gameScheduleState - should default to 'OK'
        // Missing startTimeUTC - should default to current time
      },
    };

    mockedAxios.get
      .mockResolvedValueOnce(scheduleResponse)
      .mockResolvedValueOnce(directResponse);

    const result = await nhlService.getGameStatus(mockGameId);

    expect(result).not.toBeNull();
    expect(result!.gameId).toBe(mockGameId);
    expect(result!.gameState).toBe('FUT'); // Default value
    expect(result!.gameScheduleState).toBe('OK'); // Default value
    expect(result!.startTimeUTC).toBeDefined(); // Default to current time
    expect(result!.isScheduled).toBe(true);
  });

  // Mocked behavior: Cache is cleared and API is called again
  // Input: same game ID, but cache cleared between calls
  // Expected behavior: First call hits API, cache cleared, second call hits API again
  // Expected output: API should be called twice (verified via mock call count)
  test('Cache clearing forces fresh API call', async () => {
    const mockGameId = '2024020111';
    const mockResponse = {
      data: {
        gameWeek: [
          {
            games: [
              {
                id: 2024020111,
                gameState: 'FUT',
                gameScheduleState: 'OK',
                startTimeUTC: '2024-11-02T23:00:00Z',
              },
            ],
          },
        ],
      },
    };

    mockedAxios.get.mockResolvedValue(mockResponse);

    // First call - hits API
    const result1 = await nhlService.getGameStatus(mockGameId);
    expect(result1).not.toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);

    // Second call without clearing cache - uses cache
    const result2 = await nhlService.getGameStatus(mockGameId);
    expect(result2).not.toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1); // Still 1 API call (cached)

    // Clear cache for this specific game
    nhlService.clearCache(mockGameId);

    // Third call after clearing - hits API again
    const result3 = await nhlService.getGameStatus(mockGameId);
    expect(result3).not.toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(2); // Now 2 API calls!
    expect(result3!.gameId).toBe(mockGameId);
  });

  // Mocked behavior: Non-Axios error thrown
  // Input: valid game ID
  // Expected behavior: Handles error gracefully
  // Expected output: null
  test('Handles unexpected non-Axios errors', async () => {
    const mockError = new TypeError('Unexpected error');

    mockedAxios.get.mockRejectedValueOnce(mockError);
    // Mock isAxiosError to return false so we hit the else branch
    jest.spyOn(axios, 'isAxiosError').mockReturnValueOnce(false);

    const result = await nhlService.getGameStatus('2023020001');

    expect(result).toBeNull();
    expect(mockedAxios.get).toHaveBeenCalledTimes(1);
  });
});
