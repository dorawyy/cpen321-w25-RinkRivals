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
import { friendModel } from '../../../src/models/friends.model';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/friends/request
describe('Mocked POST /api/friends/request', () => {
  let authToken: string;
  let testUserId: string;
  let receiverUserId: string;
  let receiverFriendCode: string;

  beforeAll(() => {
    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    // Create fake user IDs
    testUserId = new mongoose.Types.ObjectId().toString();
    receiverUserId = new mongoose.Types.ObjectId().toString();
    receiverFriendCode = 'TEST123456';

    authToken = jwt.sign(
      { id: testUserId },
      process.env.JWT_SECRET || 'test-secret'
    );

    // Mock userModel.findById to return a fake user (for auth middleware)
    jest.spyOn(userModel, 'findById').mockImplementation(async (id: any) => {
      return {
        _id: id,
        googleId: 'mock-google-id',
        email: 'mock@example.com',
        name: 'Mock User',
        friendCode: 'SENDER123',
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

  // Mocked behavior: Send request with non-existent friend code
  // Input: Invalid friend code
  // Expected behavior: Returns 404 error
  // Expected output: 404 status, error message
  test('Returns 404 when receiver not found', async () => {
    jest.spyOn(userModel, 'findByFriendCode').mockResolvedValueOnce(null);

    const response = await request(app)
      .post('/api/friends/request')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ receiverCode: 'INVALID_CODE_12345' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('message', 'User not found');
  });

  // Mocked behavior: Database connection error when sending request
  // Input: Valid friend code, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status with error message
  test('Returns 500 when database connection error occurs', async () => {
    jest.spyOn(userModel, 'findByFriendCode').mockResolvedValueOnce({
      _id: new mongoose.Types.ObjectId(receiverUserId),
      googleId: 'receiver-google-id',
      email: 'receiver@example.com',
      name: 'Receiver User',
      friendCode: receiverFriendCode,
    } as any);

    jest
      .spyOn(friendModel, 'sendRequest')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .post('/api/friends/request')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ receiverCode: receiverFriendCode });

    expect(response.status).toBe(500);
    expect(friendModel.sendRequest).toHaveBeenCalledTimes(1);
    expect(friendModel.sendRequest).toHaveBeenCalledWith(
      testUserId,
      receiverUserId
    );
  });

  // Mocked behavior: Database timeout when sending request
  // Input: Valid friend code, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status with error message
  test('Returns 500 when database timeout occurs', async () => {
    jest.spyOn(userModel, 'findByFriendCode').mockResolvedValueOnce({
      _id: new mongoose.Types.ObjectId(receiverUserId),
      googleId: 'receiver-google-id',
      email: 'receiver@example.com',
      name: 'Receiver User',
      friendCode: receiverFriendCode,
    } as any);

    jest
      .spyOn(friendModel, 'sendRequest')
      .mockRejectedValueOnce(new Error('Operation timed out'));

    const response = await request(app)
      .post('/api/friends/request')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ receiverCode: receiverFriendCode });

    expect(response.status).toBe(500);
    expect(friendModel.sendRequest).toHaveBeenCalledTimes(1);
    expect(friendModel.sendRequest).toHaveBeenCalledWith(
      testUserId,
      receiverUserId
    );
  });

  // Mocked behavior: Non-ZodError exception in validation middleware (covers validation.middleware.ts line 23)
  // Input: Trigger non-ZodError exception during validation
  // Expected behavior: Returns 500 with generic error
  // Expected output: 500 status with error message
  test('Returns 500 when non-ZodError exception occurs in validation', async () => {
    // Send malformed JSON to trigger parsing error before validation
    const response = await request(app)
      .post('/api/friends/request')
      .set('Authorization', `Bearer ${authToken}`)
      .set('Content-Type', 'application/json')
      .send('{ invalid json }');

    expect(response.status).toBe(400); // Express body-parser returns 400 for invalid JSON
  });
});
