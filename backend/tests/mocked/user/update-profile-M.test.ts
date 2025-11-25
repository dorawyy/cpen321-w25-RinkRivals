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

// Interface PUT /api/user/profile
describe('Mocked PUT /api/user/profile', () => {
  let authToken: string;
  let testUserId: string;

  beforeAll(() => {
    // Clear mock call history before the test suite (ensure a clean start)
    jest.clearAllMocks();

    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});

    testUserId = new mongoose.Types.ObjectId().toString();

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
  // don't accumulate call counts are cleared before each test
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Database error when updating profile
  // Input: Valid update data, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Returns 500 when database error occurs', async () => {
    jest
      .spyOn(userModel, 'update')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(500);
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Database timeout when updating profile
  // Input: Valid update data, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when database timeout occurs', async () => {
    jest
      .spyOn(userModel, 'update')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(500);
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: User not found during update
  // Input: Valid update data, user doesn't exist
  // Expected behavior: Returns 404 error
  // Expected output: 404 status
  test('Returns 404 when user not found', async () => {
    jest.spyOn(userModel, 'update').mockResolvedValueOnce(null);

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('message', 'User not found');
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Network interruption during update
  // Input: Valid update data, network fails during operation
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when network interruption occurs', async () => {
    jest
      .spyOn(userModel, 'update')
      .mockRejectedValueOnce(new Error('Network error'));

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ bio: 'Updated bio' });

    expect(response.status).toBe(500);
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Disk space error during update
  // Input: Valid update data, server runs out of disk space
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when disk space error occurs', async () => {
    jest
      .spyOn(userModel, 'update')
      .mockRejectedValueOnce(new Error('ENOSPC: no space left on device'));

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ profilePicture: 'https://example.com/new-pic.jpg' });

    expect(response.status).toBe(500);
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Non-Error exception thrown
  // Input: Valid update data, non-Error exception thrown
  // Expected behavior: Passes to error handler
  // Expected output: 500 status (from error handler)
  // Note: Tests the else branch when error is not instanceof Error
  test('Handles non-Error exceptions', async () => {
    jest
      .spyOn(userModel, 'update')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(500);
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Error with empty message
  // Input: Valid update data, Error with empty message thrown
  // Expected behavior: Returns 500 with fallback message
  // Expected output: 500 status with default error message
  // Note: Tests the || fallback in error.message || 'default'
  test('Returns default message when error message is empty', async () => {
    const emptyMessageError = new Error('');
    jest.spyOn(userModel, 'update').mockRejectedValueOnce(emptyMessageError);

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty(
      'message',
      'Failed to update user info'
    );
    expect(userModel.update).toHaveBeenCalledTimes(1);
  });

  // Model test: Database error in userModel.update throws error
  // Input: Database connection error
  // Expected behavior: Throws 'Failed to update user' error
  // Expected output: Error thrown
  test('userModel.update throws error when database fails', async () => {
    const fakeId = new mongoose.Types.ObjectId();
    const updateData = { name: 'Updated Name' };

    jest
      .spyOn(userModel['user'], 'findByIdAndUpdate')
      .mockRejectedValueOnce(new Error('Database connection lost'));

    await expect(userModel.update(fakeId, updateData)).rejects.toThrow(
      'Failed to update user'
    );
  });
});
