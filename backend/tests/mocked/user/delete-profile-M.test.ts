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
import { MediaService } from '../../../src/services/media.service';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface DELETE /api/user/profile
describe('Mocked DELETE /api/user/profile', () => {
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
  // don't accumulate call counts across tests.
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Database error when deleting profile
  // Input: Authenticated user, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Returns 500 when database error occurs', async () => {
    // Mock MediaService.deleteAllUserImages to succeed
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockResolvedValueOnce(undefined);

    // Mock userModel.delete to throw error
    jest
      .spyOn(userModel, 'delete')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
    expect(userModel.delete).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Database timeout when deleting profile
  // Input: Authenticated user, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when database timeout occurs', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockResolvedValueOnce(undefined);

    jest
      .spyOn(userModel, 'delete')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.delete).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Media service error when deleting images
  // Input: Authenticated user, image deletion fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when media service error occurs', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockRejectedValueOnce(new Error('Failed to delete images'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
    // userModel.delete should not be called if image deletion fails
    expect(userModel.delete).not.toHaveBeenCalled();
  });

  // Mocked behavior: Filesystem error when deleting user data
  // Input: Authenticated user, filesystem error occurs
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when filesystem error occurs', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockRejectedValueOnce(new Error('EACCES: permission denied'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Network interruption during deletion
  // Input: Authenticated user, network fails during operation
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when network interruption occurs', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockResolvedValueOnce(undefined);

    jest
      .spyOn(userModel, 'delete')
      .mockRejectedValueOnce(new Error('Network error'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(userModel.delete).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Partial deletion failure (images deleted but user deletion fails)
  // Input: Authenticated user, images deleted successfully but user deletion fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when user deletion fails after image deletion succeeds', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockResolvedValueOnce(undefined);

    jest
      .spyOn(userModel, 'delete')
      .mockRejectedValueOnce(new Error('Failed to delete user record'));

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
    expect(userModel.delete).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Non-Error exception thrown
  // Input: Authenticated user, non-Error exception thrown
  // Expected behavior: Passes to error handler
  // Expected output: 500 status (from error handler)
  // Note: Tests the else branch when error is not instanceof Error
  test('Handles non-Error exceptions', async () => {
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Error with empty message
  // Input: Authenticated user, Error with empty message thrown
  // Expected behavior: Returns 500 with fallback message
  // Expected output: 500 status with default error message
  // Note: Tests the || fallback in error.message || 'default'
  test('Returns default message when error message is empty', async () => {
    const emptyMessageError = new Error('');
    jest
      .spyOn(MediaService, 'deleteAllUserImages')
      .mockRejectedValueOnce(emptyMessageError);

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty('message', 'Failed to delete user');
    expect(MediaService.deleteAllUserImages).toHaveBeenCalledTimes(1);
  });

  // Model test: Database error in userModel.delete throws error
  // Input: Database connection error during findByIdAndDelete
  // Expected behavior: Throws 'Failed to delete user' error
  // Expected output: Error thrown
  test('userModel.delete throws error when database fails', async () => {
    const fakeId = new mongoose.Types.ObjectId();

    jest
      .spyOn(userModel['user'], 'findByIdAndDelete')
      .mockRejectedValueOnce(new Error('Database connection lost'));

    await expect(userModel.delete(fakeId)).rejects.toThrow(
      'Failed to delete user'
    );
  });
});
