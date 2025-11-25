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
import { OAuth2Client } from 'google-auth-library';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface POST /api/auth/signup (Integration)
describe('Unmocked POST /api/auth/signup', () => {
  let mockGoogleId: string;
  let mockEmail: string;
  let mockName: string;
  let validIdToken: string;

  beforeAll(async () => {
    await connectDB();

    // Set up mock data
    mockGoogleId = 'google-signup-test-' + Date.now();
    mockEmail = `signup-test-${Date.now()}@example.com`;
    mockName = 'Signup Test User';

    // Create a mock valid token (this will be verified by the real Google client)
    validIdToken = 'mock-valid-google-id-token';
  });

  beforeEach(async () => {
    // Clean up any existing test user before each test
    const existingUser = await userModel.findByGoogleId(mockGoogleId);
    if (existingUser) {
      await userModel.delete(existingUser._id);
    }
  });

  afterAll(async () => {
    // Cleanup test users
    const user = await userModel.findByGoogleId(mockGoogleId);
    if (user) {
      await userModel.delete(user._id);
    }
    await mongoose.connection.close();
  });

  // Integration test: Validation error for missing idToken
  // Input: Empty request body
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when idToken is missing', async () => {
    const response = await request(app).post('/api/auth/signup').send({});

    expect(response.status).toBe(400);
  });

  // Integration test: Validation error for empty idToken
  // Input: Empty string idToken
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when idToken is empty string', async () => {
    const response = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: '' });

    expect(response.status).toBe(400);
  });

  // Integration test: Invalid Google token
  // Input: Invalid/malformed Google idToken
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status, error message
  test('Returns 401 when Google token is invalid', async () => {
    const response = await request(app)
      .post('/api/auth/signup')
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
      .post('/api/auth/signup')
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
      .post('/api/auth/signup')
      .send({ idToken: 'token@#$%^&*()' });

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('message', 'Invalid Google token');
  });

  // Integration test: Extra fields in request body
  // Input: Valid token with extra fields
  // Expected behavior: Extra fields are ignored, processes normally
  // Expected output: Depends on token validity (likely 401 for mock token)
  test('Ignores extra fields in request body', async () => {
    const response = await request(app).post('/api/auth/signup').send({
      idToken: 'some-token',
      extraField: 'should be ignored',
      anotherField: 123,
    });

    // Should still validate the idToken and fail if invalid
    expect(response.status).toBe(401);
  });
});
