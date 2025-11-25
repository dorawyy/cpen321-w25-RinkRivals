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
import { authService } from '../../../src/services/auth.service';
import mongoose from 'mongoose';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/auth/signin
describe('Mocked POST /api/auth/signin', () => {
  beforeAll(() => {
    // Clear mock call history before the test suite (ensure a clean start)
    jest.clearAllMocks();

    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  // Ensure mocks and call counts are cleared before each test so spies
  // don't accumulate call counts across tests.
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Mocked behavior: Database connection error during signin
  // Input: Valid Google token, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Returns 500 when database connection error occurs', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
    expect(authService.signInWithGoogle).toHaveBeenCalledWith(
      'valid-mock-token'
    );
  });

  // Mocked behavior: Database timeout during signin
  // Input: Valid Google token, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when database timeout occurs', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: User not found
  // Input: Valid Google token for non-existent user
  // Expected behavior: Returns 404 not found
  // Expected output: 404 status, error message
  test('Returns 404 when user not found', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('User not found'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty(
      'message',
      'User not found, please sign up first.'
    );
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Invalid Google token
  // Input: Invalid Google token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when Google token is invalid', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Invalid Google token'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'invalid-token' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Failed to process user information
  // Input: Valid token, processing fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status, error message
  test('Returns 500 when failed to process user', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Failed to process user'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty(
      'message',
      'Failed to process user information'
    );
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Network interruption during Google verification
  // Input: Valid token, network fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when network interruption occurs', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Network error'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Google API rate limit
  // Input: Valid token, Google API rate limit hit
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when Google API rate limit is hit', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Rate limit exceeded'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Google service unavailable
  // Input: Valid token, Google service is down
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when Google service is unavailable', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce(new Error('Service unavailable'));

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Non-Error exception thrown
  // Input: Valid token, non-Error exception thrown
  // Expected behavior: Passes to error handler
  // Expected output: 500 status (from error handler)
  // Note: Tests the else branch when error is not instanceof Error
  test('Handles non-Error exceptions', async () => {
    jest
      .spyOn(authService, 'signInWithGoogle')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Successful signin
  // Input: Valid Google token for existing user
  // Expected behavior: Returns token for existing user
  // Expected output: 200 status, token and user data
  test('Successfully signs in user and returns token', async () => {
    const mockUser = {
      _id: new mongoose.Types.ObjectId(),
      googleId: 'google-123',
      email: 'test@example.com',
      name: 'Test User',
      friendCode: 'FRIEND123',
      createdAt: new Date(),
      updatedAt: new Date(),
    } as any;

    jest.spyOn(authService, 'signInWithGoogle').mockResolvedValueOnce({
      token: 'mock-jwt-token',
      user: mockUser,
    });

    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'User signed in successfully'
    );
    expect(response.body.data).toHaveProperty('token', 'mock-jwt-token');
    expect(response.body.data).toHaveProperty('user');
    expect(response.body.data.user).toHaveProperty('email', 'test@example.com');
    expect(authService.signInWithGoogle).toHaveBeenCalledTimes(1);
  });
});
