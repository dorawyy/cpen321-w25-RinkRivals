import mongoose, { Document } from 'mongoose';
import z from 'zod';

export type ScheduleResponse = {
  nextStartDate: string;
  previousStartDate: string;
  gameDay: GameDay[];
};

export type GameDay = {
  date: string;
  dayAbbrev: string;
  numberOfGames: number;
  datePromo: any[]; // Could refine if you know the type
  games: Game[];
};

export type Game = {
  id: number;
  season: number;
  gameType: number;
  venue: Venue;
  neutralSite: boolean;
  startTimeUTC: string;
  easternUTCOffset: string;
  venueUTCOffset: string;
  venueTimezone: string;
  gameState: string;
  gameScheduleState: string;
  tvBroadcasts: TvBroadcast[];
  awayTeam: Team;
  homeTeam: Team;
  periodDescriptor: PeriodDescriptor;
  ticketsLink?: string;
  ticketsLinkFr?: string;
  gameCenterLink?: string;
};

export type Venue = {
  default: string;
  fr?: string;
};

export type TvBroadcast = {
  id: number;
  market: string;
  countryCode: string;
  network: string;
  sequenceNumber: number;
};

export type Team = {
  id: number;
  commonName: Name;
  placeName: Name;
  placeNameWithPreposition: Name;
  abbrev: string;
  logo: string;
  darkLogo: string;
  awaySplitSquad?: boolean;
  homeSplitSquad?: boolean;
  radioLink: string;
  odds?: Odds[];
};

export type Name = {
  default: string;
  fr?: string;
};

export type Odds = {
  providerId: number;
  value: string;
};

export type PeriodDescriptor = {
  number: number;
  periodType: string;
  maxRegulationPeriods: number;
};

// --- EventCondition matching frontend ---
export enum EventCategory {
  FORWARD = 'FORWARD',
  DEFENSE = 'DEFENSE',
  GOALIE = 'GOALIE',
  TEAM = 'TEAM',
  PENALTY = 'PENALTY',
}

export enum ComparisonType {
  GREATER_THAN = 'GREATER_THAN',
  LESS_THAN = 'LESS_THAN',
  EQUAL = 'EQUAL',
}

export type EventCondition = {
  id: string;
  category: EventCategory;
  subject: string; // e.g. "goals", "assists", "penaltyMinutes"
  comparison: ComparisonType;
  threshold: number;
  teamAbbrev?: string; // e.g. "VAN" or "BOS"
  playerId?: number; // optional for player-specific events
  playerName?: string; // cached for label generation
};

// Interface used internally by Mongoose
export interface ITicket extends Document {
  userId: string;
  name: string;
  game: Game;
  events: EventCondition[];
  crossedOff: boolean[];
  score: BingoTicketScore;
  createdAt: Date;
  updatedAt: Date;
}

// Zod schema for validation when creating tickets
export const createTicketSchema = z.object({
  userId: z.string().min(1, 'User ID required'),
  name: z.string().min(1, 'Name required'),
  game: z.object({
    id: z.number(),
    homeTeam: z.object({
      abbrev: z.string().min(1, 'Home team abbrev required'),
    }),
    awayTeam: z.object({
      abbrev: z.string().min(1, 'Away team abbrev required'),
    }),
  }),
  events: z
    .array(
      z.object({
        id: z.string(),
        category: z.nativeEnum(EventCategory),
        subject: z.string(),
        comparison: z.nativeEnum(ComparisonType),
        threshold: z.number(),
        teamAbbrev: z.string().optional(),
        playerId: z.number().optional(),
        playerName: z.string().optional(),
      })
    )
    .length(9, 'Exactly 9 events required'),
  // score is optional on create; backend will compute defaults if omitted
  score: z
    .object({
      noCrossedOff: z.number().min(0),
      noRows: z.number().min(0),
      noColumns: z.number().min(0),
      noCrosses: z.number().min(0),
      total: z.number().min(0),
    })
    .optional(),
});

export type CreateTicketBody = z.infer<typeof createTicketSchema>;
export type TicketType = CreateTicketBody;

export type BingoTicketScore = {
  noCrossedOff: number;
  noRows: number;
  noColumns: number;
  noCrosses: number;
  total: number;
};

export type TicketResponse = TicketType & {
  crossedOff: boolean[];
  score?: BingoTicketScore;
};
