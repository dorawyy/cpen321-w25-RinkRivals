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
import router from '../../../src/routes/routes';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { userModel } from '../../../src/models/user.model';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface GET /api/user/:id
describe('Mocked GET /api/user/:id', () => {
  let authToken: string;
  let testUserId: string;
  let targetUserId: string;

  beforeAll(() => {
    // Clear mock call history before the test suite (ensure a clean start)
    jest.clearAllMocks();

    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    testUserId = new mongoose.Types.ObjectId().toString();
    targetUserId = new mongoose.Types.ObjectId().toString();

    authToken = jwt.sign(
      { id: testUserId },
      process.env.JWT_SECRET || 'test-secret'
    );

    // Mock userModel.findById for auth middleware
    jest.spyOn(userModel, 'findById').mockImplementation(async (id: any) => {
      return {
        _id: id,
        googleId: 'mock-google-id',
        email: 'mock@example.com',
        name: 'Mock User',
        friendCode: 'MOCK123456',
      } as any;
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

  // Mocked behavior: Database error when fetching user info
  // Input: Valid user ID, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Returns 500 when database error occurs', async () => {
    jest
      .spyOn(userModel, 'findUserInfoById')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.findUserInfoById).toHaveBeenCalledTimes(1);
    expect(userModel.findUserInfoById).toHaveBeenCalledWith(targetUserId);
  });

  // Mocked behavior: Database timeout when fetching user info
  // Input: Valid user ID, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when database timeout occurs', async () => {
    jest
      .spyOn(userModel, 'findUserInfoById')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.findUserInfoById).toHaveBeenCalledTimes(1);
    expect(userModel.findUserInfoById).toHaveBeenCalledWith(targetUserId);
  });

  // Mocked behavior: Network interruption when fetching user info
  // Input: Valid user ID, network fails during query
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when network interruption occurs', async () => {
    jest
      .spyOn(userModel, 'findUserInfoById')
      .mockRejectedValueOnce(new Error('Network error'));

    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.findUserInfoById).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Non-Error exception thrown
  // Input: Valid user ID, non-Error exception thrown
  // Expected behavior: Passes to error handler
  // Expected output: 500 status (from error handler)
  // Note: Tests the else branch when error is not instanceof Error
  test('Handles non-Error exceptions', async () => {
    jest
      .spyOn(userModel, 'findUserInfoById')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.findUserInfoById).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Error with empty message
  // Input: Valid user ID, Error with empty message thrown
  // Expected behavior: Returns 500 with fallback message
  // Expected output: 500 status with default error message
  // Note: Tests the || fallback in error.message || 'default'
  test('Returns default message when error message is empty', async () => {
    const emptyMessageError = new Error('');
    jest
      .spyOn(userModel, 'findUserInfoById')
      .mockRejectedValueOnce(emptyMessageError);

    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty(
      'message',
      'Failed to fetch user info by ID'
    );
    expect(userModel.findUserInfoById).toHaveBeenCalledTimes(1);
  });
});
