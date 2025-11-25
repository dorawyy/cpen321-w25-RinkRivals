import {
  describe,
  expect,
  test,
  beforeAll,
  afterAll,
  beforeEach,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import router from '../../../src/routes/routes';
import mongoose from 'mongoose';
import { userModel } from '../../../src/models/user.model';
import { connectDB } from '../../../src/config/database';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/auth/signin (Integration)
describe('Unmocked POST /api/auth/signin', () => {
  let mockGoogleId: string;
  let mockEmail: string;
  let mockName: string;
  let testUserId: string;

  beforeAll(async () => {
    await connectDB();

    // Set up mock data
    mockGoogleId = 'google-signin-test-' + Date.now();
    mockEmail = `signin-test-${Date.now()}@example.com`;
    mockName = 'Signin Test User';

    // Create a test user for signin tests
    const testUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: mockName,
    });
    testUserId = testUser._id.toString();
  });

  afterAll(async () => {
    // Cleanup test user
    if (testUserId) {
      await userModel.delete(new mongoose.Types.ObjectId(testUserId));
    }
    await mongoose.connection.close();
  });

  // Integration test: Validation error for missing idToken
  // Input: Empty request body
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when idToken is missing', async () => {
    const response = await request(app).post('/api/auth/signin').send({});

    expect(response.status).toBe(400);
  });

  // Integration test: Validation error for empty idToken
  // Input: Empty string idToken
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when idToken is empty string', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: '' });

    expect(response.status).toBe(400);
  });

  // Integration test: Invalid Google token
  // Input: Invalid/malformed Google idToken
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when Google token is invalid', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'invalid-token-12345' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
  });

  // Integration test: Malformed token format
  // Input: Completely malformed token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when token format is malformed', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'not-a-jwt-token' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
  });

  // Integration test: Token with special characters
  // Input: Token containing special characters
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when token contains special characters', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token@#$%^&*()' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
  });

  // Integration test: Extra fields in request body
  // Input: Valid token with extra fields
  // Expected behavior: Extra fields are ignored, processes normally
  // Expected output: Depends on token validity (likely 401 for mock token)
  test('Ignores extra fields in request body', async () => {
    const response = await request(app).post('/api/auth/signin').send({
      idToken: 'some-token',
      extraField: 'should be ignored',
      anotherField: 123,
    });

    // Should still validate the idToken and fail if invalid
    expect(response.status).toBe(401);
  });

  // Integration test: Null idToken
  // Input: idToken set to null
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status
  test('Returns 400 when idToken is null', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: null });

    expect(response.status).toBe(400);
  });

  // Integration test: Numeric idToken
  // Input: idToken set to a number
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status
  test('Returns 400 when idToken is a number', async () => {
    const response = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 12345 });

    expect(response.status).toBe(400);
  });
});
