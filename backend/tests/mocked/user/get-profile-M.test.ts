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

// Interface GET /api/user/profile
describe('Mocked GET /api/user/profile', () => {
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
        bio: 'Test bio',
        profilePicture: 'https://example.com/pic.jpg',
        createdAt: new Date(),
        updatedAt: new Date(),
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

  // Mocked behavior: Get profile returns successfully
  // Input: Authenticated user
  // Expected behavior: Returns user profile data
  // Expected output: 200 status with user data
  test('Successfully returns user profile', async () => {
    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'Profile fetched successfully'
    );
    expect(response.body.data).toHaveProperty('user');
    expect(response.body.data.user).toHaveProperty('_id');
    expect(response.body.data.user).toHaveProperty('name', 'Mock User');
    expect(userModel.findById).toHaveBeenCalledTimes(1);
  });

  // Model test: Database error in userModel.findByFriendCode throws error
  // Input: Database connection error
  // Expected behavior: Throws 'Failed to find user' error
  // Expected output: Error thrown
  test('userModel.findByFriendCode throws error when database fails', async () => {
    const friendCode = 'TEST123456';

    jest
      .spyOn(userModel['user'], 'findOne')
      .mockRejectedValueOnce(new Error('Database connection lost'));

    await expect(userModel.findByFriendCode(friendCode)).rejects.toThrow(
      'Failed to find user'
    );
  });

  // Model test: Database error during create operation (non-validation error)
  // Input: Database connection error during create
  // Expected behavior: Throws 'Failed to update user' error (note: error message is wrong in code)
  // Expected output: Error thrown
  test('userModel.create throws error when database create fails', async () => {
    // Mock findOne to return null (unique friend code)
    jest.spyOn(userModel['user'], 'findOne').mockResolvedValueOnce(null);

    // Mock create to throw a database error
    jest
      .spyOn(userModel['user'], 'create')
      .mockRejectedValueOnce(new Error('Database connection lost'));

    await expect(
      userModel.create({
        googleId: 'google-123',
        email: 'test@example.com',
        name: 'Test User',
      })
    ).rejects.toThrow('Failed to update user');
  });

  // Model test: Friend code collision handled during create
  // Input: First generated friend code already exists, second is unique
  // Expected behavior: Loops until unique code found, creates user successfully
  // Expected output: User created with unique friend code
  test('userModel.create handles friend code collision', async () => {
    const mockUser = {
      _id: new mongoose.Types.ObjectId(),
      googleId: 'google-collision-test',
      email: 'collision@test.com',
      name: 'Collision Test User',
      friendCode: 'UNIQUE1234',
    };

    // First findOne returns existing user (collision), second returns null (unique)
    jest
      .spyOn(userModel['user'], 'findOne')
      .mockResolvedValueOnce({ friendCode: 'COLIDE1234' } as any) // Collision
      .mockResolvedValueOnce(null); // Unique

    // Mock create to return the new user
    jest
      .spyOn(userModel['user'], 'create')
      .mockResolvedValueOnce(mockUser as any);

    const result = await userModel.create({
      googleId: 'google-collision-test',
      email: 'collision@test.com',
      name: 'Collision Test User',
    });

    expect(result).toBeDefined();
    expect(userModel['user'].findOne).toHaveBeenCalledTimes(2); // Called twice due to collision
  });
});
