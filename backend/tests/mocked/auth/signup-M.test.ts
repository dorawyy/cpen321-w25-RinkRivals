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
import { userModel } from '../../../src/models/user.model';
import mongoose from 'mongoose';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/auth/signup
describe('Mocked POST /api/auth/signup', () => {
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

  // Mocked behavior: Database connection error during signup
  // Input: Valid Google token, database connection fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  // Note: This tests error handling that can't be provoked in unmocked tests
  test('Returns 500 when database connection error occurs', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Database connection failed'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
    expect(authService.signUpWithGoogle).toHaveBeenCalledWith(
      'valid-mock-token'
    );
  });

  // Mocked behavior: Database timeout during signup
  // Input: Valid Google token, database operation times out
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when database timeout occurs', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Query timeout'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: User already exists
  // Input: Valid Google token for existing user
  // Expected behavior: Returns 409 conflict
  // Expected output: 409 status, error message
  test('Returns 409 when user already exists', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('User already exists'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(409);
    expect(response.body).toHaveProperty(
      'message',
      'User already exists, please sign in instead.'
    );
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Invalid Google token
  // Input: Invalid Google token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when Google token is invalid', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Invalid Google token'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'invalid-token' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Failed to process user information
  // Input: Valid token, processing fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status, error message
  test('Returns 500 when failed to process user', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Failed to process user'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty(
      'message',
      'Failed to process user information'
    );
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Network interruption during Google verification
  // Input: Valid token, network fails
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when network interruption occurs', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Network error'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Google API rate limit
  // Input: Valid token, Google API rate limit hit
  // Expected behavior: Returns 500 error
  // Expected output: 500 status
  test('Returns 500 when Google API rate limit is hit', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce(new Error('Rate limit exceeded'));

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Non-Error exception thrown
  // Input: Valid token, non-Error exception thrown
  // Expected behavior: Passes to error handler
  // Expected output: 500 status (from error handler)
  // Note: Tests the else branch when error is not instanceof Error
  test('Handles non-Error exceptions', async () => {
    jest
      .spyOn(authService, 'signUpWithGoogle')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(500);
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });

  // Mocked behavior: Successful signup
  // Input: Valid Google token
  // Expected behavior: Creates user and returns token
  // Expected output: 201 status, token and user data
  test('Successfully creates user and returns token', async () => {
    const mockUser = {
      _id: new mongoose.Types.ObjectId(),
      googleId: 'google-123',
      email: 'test@example.com',
      name: 'Test User',
      friendCode: 'FRIEND123',
      createdAt: new Date(),
      updatedAt: new Date(),
    } as any;

    jest.spyOn(authService, 'signUpWithGoogle').mockResolvedValueOnce({
      token: 'mock-jwt-token',
      user: mockUser,
    });

    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-mock-token' });

    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty(
      'message',
      'User signed up successfully'
    );
    expect(response.body.data).toHaveProperty('token', 'mock-jwt-token');
    expect(response.body.data).toHaveProperty('user');
    expect(response.body.data.user).toHaveProperty('email', 'test@example.com');
    expect(authService.signUpWithGoogle).toHaveBeenCalledTimes(1);
  });
});

// Direct AuthService Tests
describe('AuthService.signUpWithGoogle - Service Level Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Service test: Invalid token payload (null payload)
  // Input: Google token that returns null payload
  // Expected behavior: Throws 'Invalid Google token' error
  // Expected output: Error thrown
  test('Throws error when Google token payload is null', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockResolvedValueOnce({
      getPayload: () => null,
    });

    await expect(authService.signUpWithGoogle('invalid-token')).rejects.toThrow(
      'Invalid Google token'
    );
  });

  // Service test: Missing email in payload
  // Input: Google token with payload missing email
  // Expected behavior: Throws 'Invalid Google token' error
  // Expected output: Error thrown
  test('Throws error when email is missing from Google payload', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockResolvedValueOnce({
      getPayload: () => ({
        sub: 'google-123',
        name: 'Test User',
        // email is missing
      }),
    });

    await expect(
      authService.signUpWithGoogle('token-without-email')
    ).rejects.toThrow('Invalid Google token');
  });

  // Service test: Missing name in payload
  // Input: Google token with payload missing name
  // Expected behavior: Throws 'Invalid Google token' error
  // Expected output: Error thrown
  test('Throws error when name is missing from Google payload', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockResolvedValueOnce({
      getPayload: () => ({
        sub: 'google-123',
        email: 'test@example.com',
        // name is missing
      }),
    });

    await expect(
      authService.signUpWithGoogle('token-without-name')
    ).rejects.toThrow('Invalid Google token');
  });

  // Service test: Google verifyIdToken throws error
  // Input: Invalid token that Google rejects
  // Expected behavior: Throws 'Invalid Google token' error
  // Expected output: Error thrown
  test('Throws error when Google verifyIdToken fails', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockRejectedValueOnce(new Error('Token verification failed'));

    await expect(authService.signUpWithGoogle('bad-token')).rejects.toThrow(
      'Invalid Google token'
    );
  });

  // Service test: User already exists during signup
  // Input: Valid token for existing user
  // Expected behavior: Throws 'User already exists' error
  // Expected output: Error thrown
  test('Throws error when user already exists', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockResolvedValueOnce({
      getPayload: () => ({
        sub: 'google-existing',
        email: 'existing@example.com',
        name: 'Existing User',
        picture: 'https://example.com/pic.jpg',
      }),
    });

    // Mock userModel to return existing user
    jest.spyOn(userModel, 'findByGoogleId').mockResolvedValueOnce({
      _id: new mongoose.Types.ObjectId(),
      googleId: 'google-existing',
      email: 'existing@example.com',
      name: 'Existing User',
    } as any);

    await expect(authService.signUpWithGoogle('valid-token')).rejects.toThrow(
      'User already exists'
    );
  });

  // Service test: Successful signup with valid token
  // Input: Valid Google token for new user
  // Expected behavior: Creates user and returns token
  // Expected output: AuthResult with token and user
  test('Successfully creates user with valid token', async () => {
    (
      jest.spyOn(authService['googleClient'], 'verifyIdToken') as any
    ).mockResolvedValueOnce({
      getPayload: () => ({
        sub: 'google-new-user',
        email: 'newuser@example.com',
        name: 'New User',
        picture: 'https://example.com/pic.jpg',
      }),
    } as any);

    // Mock userModel to return null (user doesn't exist)
    jest.spyOn(userModel, 'findByGoogleId').mockResolvedValueOnce(null);

    // Mock userModel.create to return new user
    const mockUser = {
      _id: new mongoose.Types.ObjectId(),
      googleId: 'google-new-user',
      email: 'newuser@example.com',
      name: 'New User',
      friendCode: 'NEW123456',
      profilePicture: 'https://example.com/pic.jpg',
      createdAt: new Date(),
      updatedAt: new Date(),
    } as any;

    jest.spyOn(userModel, 'create').mockResolvedValueOnce(mockUser);

    const result = await authService.signUpWithGoogle('valid-token');

    expect(result).toHaveProperty('token');
    expect(result).toHaveProperty('user');
    expect(result.user.email).toBe('newuser@example.com');
    expect(result.token).toBeTruthy();
  });
});
