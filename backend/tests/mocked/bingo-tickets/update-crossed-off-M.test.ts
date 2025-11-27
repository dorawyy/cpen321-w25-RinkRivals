import {
  describe,
  expect,
  test,
  jest,
  beforeAll,
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
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing (same setup as index.ts)
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface PUT /api/tickets/:id/crossedOff
describe('Mocked PUT /api/tickets/crossedOff/:id', () => {
  let authToken: string;
  let testUserId: string;
  let testTicketId: string;

  // For mocked tests we do not connect to a real DB; instead mock user lookup
  beforeAll(() => {
    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    // create a fake user id and token
    testUserId = new mongoose.Types.ObjectId().toString();
    testTicketId = new mongoose.Types.ObjectId().toString();
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

  // Restore mocks after tests
  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Ticket.findByIdAndUpdate throws an error
  // Input: valid ticket id and crossedOff array
  // Expected status code: 500
  // Expected behavior: the error was handled gracefully
  // Expected output: Error object with message
  test('Database throws when Ticket.findByIdAndUpdate fails', async () => {
    // Arrange: mock Ticket.findByIdAndUpdate to throw
    jest.spyOn(Ticket, 'findByIdAndUpdate').mockImplementationOnce(() => {
      throw new Error('Forced DB error');
    });

    const crossedOff = [
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false,
      true,
    ];

    // Act
    const res = await request(app)
      .put(`/api/tickets/crossedOff/${testTicketId}`)
      .set('Authorization', `Bearer ${authToken}`)
      .send({ crossedOff });

    // Assert: controller should return 500 on DB error
    expect(res.status).toBe(500);
    // New behavior: controller computes score and updates crossedOff + score in one call
    const expectedScore = {
      noCrossedOff: crossedOff.filter(Boolean).length,
      noRows: 0,
      noColumns: 0,
      noCrosses: 2,
      total: crossedOff.filter(Boolean).length + 2 * 3,
    };
    expect(Ticket.findByIdAndUpdate).toHaveBeenCalledWith(
      testTicketId,
      { crossedOff, score: expectedScore },
      { new: true }
    );
    expect(res.body).toHaveProperty('error');
  });

  // Test: computeTicketScore handles non-array input (covers score.util.ts line 22)
  // Input: Non-array value passed to computeTicketScore
  // Expected behavior: normalizeCrossedOff returns array of false
  // Expected output: Score computed with all false values
  test('computeTicketScore handles non-array input gracefully', () => {
    const { computeTicketScore } = require('../../../src/utils/score.util');

    // Pass non-array inputs to trigger the !Array.isArray check
    const result1 = computeTicketScore(null as any);
    expect(result1.noCrossedOff).toBe(0);
    expect(result1.total).toBe(0);

    const result2 = computeTicketScore(undefined as any);
    expect(result2.noCrossedOff).toBe(0);
    expect(result2.total).toBe(0);

    const result3 = computeTicketScore('not-an-array' as any);
    expect(result3.noCrossedOff).toBe(0);
    expect(result3.total).toBe(0);

    const result4 = computeTicketScore({ invalid: 'object' } as any);
    expect(result4.noCrossedOff).toBe(0);
    expect(result4.total).toBe(0);
  });
});
