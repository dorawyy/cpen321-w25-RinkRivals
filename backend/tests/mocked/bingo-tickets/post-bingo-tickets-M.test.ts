import {
  describe,
  expect,
  test,
  jest,
  beforeAll,
  beforeEach,
  afterAll,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import router from '../../../src/routes/routes';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { userModel } from '../../../src/models/user.model';
import { Ticket } from '../../../src/models/tickets.model';
import { EventCondition, TicketType } from '../../../src/types/tickets.types';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing (same setup as index.ts)
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/tickets
describe('Mocked POST /api/tickets', () => {
  let authToken: string;
  let testUserId: string;

  // For mocked tests we do not connect to a real DB; instead mock user lookup
  beforeAll(() => {
    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    // create a fake user id and token
    testUserId = new mongoose.Types.ObjectId().toString();
    authToken = jwt.sign(
      { id: testUserId },
      process.env.JWT_SECRET || 'test-secret'
    );

    // Ensure userModel.findById returns a fake user so authenticateToken passes
    jest.spyOn(userModel, 'findById').mockImplementation(async (id: any) => {
      // return an object that looks like a user document
      return {
        _id: id,
        googleId: 'mock-google-id',
        email: 'mock@example.com',
        name: 'Mock User',
      } as any;
    });
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  // Restore mocks after tests
  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Ticket.create throws an error
  // Input: valid ticket data
  // Expected status code: 500
  // Expected behavior: the error was handled gracefully
  // Expected output: None
  test('Database throws when Ticket.create fails', async () => {
    // Arrange: mock Ticket.create to throw
    jest.spyOn(Ticket, 'create').mockImplementationOnce(() => {
      throw new Error('Forced DB error');
    });

    const events = Array.from({ length: 9 }, (_, i) => ({
      id: `e${i}`,
      category: 'FORWARD' as any,
      subject: 'goals',
      comparison: 'GREATER_THAN' as any,
      threshold: 1,
    }));

    const validTicket: TicketType = {
      userId: testUserId,
      name: 'Mock Ticket',
      game: { id: 1, homeTeam: { abbrev: 'HT' }, awayTeam: { abbrev: 'AT' } },
      events: events as EventCondition[],
    };

    // Act
    const res = await request(app)
      .post('/api/tickets')
      .set('Authorization', `Bearer ${authToken}`)
      .send(validTicket);

    // Assert: controller should return 500 on DB error
    expect(res.status).toBe(500);
    expect(Ticket.create).toHaveBeenCalledTimes(1);
    expect(res.body).toHaveProperty('message', 'Server error');
  });

  // Mocked behavior: Non-ZodError exception in validation middleware (covers validation.middleware.ts line 23)
  // Input: Mock schema.parse to throw non-ZodError by spying on createTicketSchema
  // Expected behavior: Returns 500 with generic error
  // Expected output: 500 status with validation processing failed message
  test('Returns 500 when non-ZodError exception occurs in validation', async () => {
    const ticketsTypes = require('../../../src/types/tickets.types');

    // Spy on createTicketSchema parse and throw a non-ZodError
    const mockParse = jest
      .spyOn(ticketsTypes.createTicketSchema, 'parse')
      .mockImplementationOnce(() => {
        throw new TypeError('Unexpected validation error');
      });

    const events: EventCondition[] = Array(9).fill({
      id: '1',
      category: 'GOAL',
      subject: 'test',
      comparison: 'GREATER_THAN',
      threshold: 0,
    });

    const validTicket: TicketType = {
      userId: testUserId,
      name: 'Mock Ticket',
      game: { id: 1, homeTeam: { abbrev: 'HT' }, awayTeam: { abbrev: 'AT' } },
      events: events,
    };

    const res = await request(app)
      .post('/api/tickets')
      .set('Authorization', `Bearer ${authToken}`)
      .send(validTicket);

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('error', 'Internal server error');
    expect(res.body).toHaveProperty('message', 'Validation processing failed');

    mockParse.mockRestore();
  });
});
