import {
  describe,
  expect,
  test,
  jest,
  beforeAll,
  afterAll,
  beforeEach,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import { authenticateToken } from '../../../src/middleware/auth.middleware';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { userModel } from '../../../src/models/user.model';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());

// Test route that uses authentication
app.get('/test-auth', authenticateToken, (req, res) => {
  res.status(200).json({ message: 'Authenticated', user: req.user });
});

// Interface: auth.middleware.ts (Mocked)
describe('Mocked auth.middleware.ts', () => {
  let testUserId: string;
  let validToken: string;

  beforeAll(() => {
    // Clear mock call history before the test suite (ensure a clean start)
    jest.clearAllMocks();

    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    testUserId = new mongoose.Types.ObjectId().toString();

    // Generate valid token
    validToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });

    // Mock userModel.findById for successful authentication
    jest.spyOn(userModel, 'findById').mockImplementation(async (id: any) => {
      if (id.toString() === testUserId) {
        return {
          _id: id,
          googleId: 'mock-google-id',
          email: 'mock@example.com',
          name: 'Mock User',
          friendCode: 'MOCK123456',
        } as any;
      }
      return null;
    });
  });

  // Ensure mocks and call counts are cleared before each test so spies
  // don't accumulate call counts across tests.
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Token with missing id field
  // Input: Valid JWT token structure but missing id field
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, token verification failed error
  // Note: This tests the decoded.id check that's hard to trigger in unmocked tests
  test('Returns 401 when token payload is missing id field', async () => {
    // Create token without id field
    const tokenWithoutId = jwt.sign(
      { someOtherField: 'value' },
      process.env.JWT_SECRET!,
      { expiresIn: '1h' }
    );

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${tokenWithoutId}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Invalid token');
    expect(response.body).toHaveProperty(
      'message',
      'Token verification failed'
    );
  });

  // Mocked behavior: Token with null id
  // Input: JWT token with null id
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, token verification failed error
  test('Returns 401 when token payload has null id', async () => {
    const tokenWithNullId = jwt.sign({ id: null }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${tokenWithNullId}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Invalid token');
    expect(response.body).toHaveProperty(
      'message',
      'Token verification failed'
    );
  });

  // Mocked behavior: Database error when finding user
  // Input: Valid token, database lookup fails
  // Expected behavior: Error propagates to error handler
  // Expected output: Error thrown (not 401)
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Propagates error when database lookup fails', async () => {
    jest
      .spyOn(userModel, 'findById')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${validToken}`);

    // The error should be passed to next(), resulting in a 500 or error response
    // The exact status depends on your error handler middleware
    expect(response.status).not.toBe(200);
  });

  // Mocked behavior: Database timeout when finding user
  // Input: Valid token, database query times out
  // Expected behavior: Error propagates to error handler
  // Expected output: Error response
  test('Propagates error when database timeout occurs', async () => {
    jest
      .spyOn(userModel, 'findById')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${validToken}`);

    expect(response.status).not.toBe(200);
  });

  // Mocked behavior: JWT verification throws unexpected error
  // Input: Token causes unexpected JWT error
  // Expected behavior: Returns 401 with generic invalid token message
  // Expected output: 401 status
  test('Handles unexpected JWT errors gracefully', async () => {
    // This would be caught by JsonWebTokenError catch block
    const malformedToken = 'not.a.valid.jwt.token';

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${malformedToken}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Invalid token');
  });
});
