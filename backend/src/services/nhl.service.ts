import axios from 'axios';
import logger from '../utils/logger.util';

// NHL API base URL - using the new NHL API (as of 2024+)
const NHL_API_BASE = 'https://api-web.nhle.com/v1';

export interface GameStatus {
  gameId: string;
  gameState: string; // "FUT" (future), "LIVE", "CRIT" (critical/close), "OFF" (official/final), etc.
  gameScheduleState: string; // "OK", "POSTPONED", etc.
  startTimeUTC: string;
  detailedState?: string;
  isLive: boolean;
  isFinished: boolean;
  isScheduled: boolean;
}

export class NHLService {
  private cache: Map<string, { data: GameStatus; timestamp: number }> =
    new Map();
  private readonly CACHE_TTL = 30000; // 30 seconds cache

  /**
   * Fetch game status from NHL API
   * @param gameId - The NHL game ID (gamePk)
   * @returns GameStatus object with current game state
   */
  async getGameStatus(gameId: string): Promise<GameStatus | null> {
    try {
      // Check cache first
      const cached = this.cache.get(gameId);
      if (cached && Date.now() - cached.timestamp < this.CACHE_TTL) {
        logger.debug(`Using cached game status for ${gameId}`);
        return cached.data;
      }

      // Fetch from NHL API
      // The new NHL API uses schedule endpoints that include game state
      const url = `${NHL_API_BASE}/schedule/now`;
      logger.debug(`Fetching NHL schedule from: ${url}`);

      const response = await axios.get(url, {
        timeout: 10000,
        headers: {
          'User-Agent': 'Hockey-Prediction-App/1.0',
        },
      });

      // Find the specific game in the schedule
      let gameData: any = null;
      if (response.data.gameWeek) {
        for (const day of response.data.gameWeek) {
          if (day.games) {
            gameData = day.games.find(
              (g: any) => g.id.toString() === gameId.toString()
            );
            if (gameData) break;
          }
        }
      }

      if (!gameData) {
        // Game not found in current schedule, try fetching specific game
        logger.warn(
          `Game ${gameId} not found in current schedule, trying specific endpoint`
        );
        return await this.getGameStatusDirect(gameId);
      }

      // Apply default values before using in helper methods
      const gameState = gameData.gameState || 'FUT';
      const gameScheduleState = gameData.gameScheduleState || 'OK';

      const status: GameStatus = {
        gameId: gameData.id.toString(),
        gameState: gameState,
        gameScheduleState: gameScheduleState,
        startTimeUTC: gameData.startTimeUTC,
        detailedState: gameData.gameScheduleState,
        isLive: this.isGameLive(gameState),
        isFinished: this.isGameFinished(gameState),
        isScheduled: this.isGameScheduled(gameState),
      };

      // Cache the result
      this.cache.set(gameId, { data: status, timestamp: Date.now() });

      logger.info(
        `Game ${gameId} status: ${status.gameState} (Live: ${status.isLive}, Finished: ${status.isFinished})`
      );

      return status;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        logger.error(
          `Error fetching game ${gameId} from NHL API: ${error.message}`
        );
        if (error.response) {
          logger.error(`Response status: ${error.response.status}`);
          logger.error(`Response data: ${JSON.stringify(error.response.data)}`);
        }
      } else {
        logger.error(`Unexpected error fetching game ${gameId}:`, error);
      }
      return null;
    }
  }

  /**
   * Fetch game status directly from game-specific endpoint
   * @param gameId - The NHL game ID
   */
  private async getGameStatusDirect(
    gameId: string
  ): Promise<GameStatus | null> {
    try {
      // Try the game-specific landing endpoint
      const url = `${NHL_API_BASE}/gamecenter/${gameId}/landing`;
      logger.debug(`Fetching game directly from: ${url}`);

      const response = await axios.get(url, {
        timeout: 10000,
        headers: {
          'User-Agent': 'Hockey-Prediction-App/1.0',
        },
      });

      if (!response.data) {
        logger.warn(`No data returned for game ${gameId}`);
        return null;
      }

      const gameState = response.data.gameState || 'FUT';
      const status: GameStatus = {
        gameId: gameId,
        gameState: gameState,
        gameScheduleState: response.data.gameScheduleState || 'OK',
        startTimeUTC: response.data.startTimeUTC || new Date().toISOString(),
        detailedState: response.data.gameScheduleState,
        isLive: this.isGameLive(gameState),
        isFinished: this.isGameFinished(gameState),
        isScheduled: this.isGameScheduled(gameState),
      };

      // Cache the result
      this.cache.set(gameId, { data: status, timestamp: Date.now() });

      return status;
    } catch (error) {
      logger.error(`Error fetching game ${gameId} directly:`, error);
      return null;
    }
  }

  /**
   * Check if game is currently live/in progress
   */
  private isGameLive(gameState: string): boolean {
    const liveStates = ['LIVE', 'CRIT'];
    return liveStates.includes(gameState.toUpperCase());
  }

  /**
   * Check if game is finished
   */
  private isGameFinished(gameState: string): boolean {
    const finishedStates = ['OFF', 'FINAL'];
    return finishedStates.includes(gameState.toUpperCase());
  }

  /**
   * Check if game is scheduled (future)
   */
  private isGameScheduled(gameState: string): boolean {
    const scheduledStates = ['FUT', 'SCHEDULED', 'PRE'];
    return scheduledStates.includes(gameState.toUpperCase());
  }

  /**
   * Clear cache for specific game or all games
   */
  clearCache(gameId?: string) {
    if (gameId) {
      this.cache.delete(gameId);
      logger.debug(`Cleared cache for game ${gameId}`);
    } else {
      this.cache.clear();
      logger.debug('Cleared all game cache');
    }
  }

  /**
   * Get time until game starts (in milliseconds)
   * @param startTimeUTC - ISO 8601 UTC timestamp
   * @returns milliseconds until game starts (negative if already started)
   */
  getTimeUntilStart(startTimeUTC: string): number {
    const startTime = new Date(startTimeUTC).getTime();
    const now = Date.now();
    return startTime - now;
  }
}

// Export singleton instance
export const nhlService = new NHLService();
export default nhlService;
