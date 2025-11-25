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
import { connectDB } from '../../../src/config/database';
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

// Interface: auth.middleware.ts (Integration)
describe('Unmocked auth.middleware.ts', () => {
  let testUserId: string;
  let validToken: string;

  beforeAll(async () => {
    await connectDB();

    // Create test user
    const testUser = await userModel.create({
      email: 'auth-middleware-test@test.com',
      name: 'Auth Middleware Test User',
      googleId: 'google-auth-middleware-test-' + Date.now(),
    });
    testUserId = testUser._id.toString();

    // Generate valid token
    validToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });
  });

  afterAll(async () => {
    // Cleanup test user
    await userModel.delete(new mongoose.Types.ObjectId(testUserId));
    await mongoose.connection.close();
  });

  // Integration test: Successfully authenticates with valid token
  // Input: Valid JWT token in Authorization header
  // Expected behavior: Allows request to proceed
  // Expected output: 200 status, authenticated response
  test('Successfully authenticates with valid token', async () => {
    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${validToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty('message', 'Authenticated');
    expect(response.body.user).toHaveProperty('_id', testUserId);
  });

  // Integration test: No token provided
  // Input: No Authorization header
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, no token error message
  test('Returns 401 when no token provided', async () => {
    const response = await request(app).get('/test-auth');

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Access denied');
    expect(response.body).toHaveProperty('message', 'No token provided');
  });

  // Integration test: Token without Bearer prefix
  // Input: Token in Authorization header without Bearer prefix
  // Expected behavior: Returns 401 error (no token extracted)
  // Expected output: 401 status
  test('Returns 401 when token has no Bearer prefix', async () => {
    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', validToken);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Access denied');
  });

  // Integration test: Malformed token
  // Input: Invalid JWT token format
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, invalid token error
  test('Returns 401 when token is malformed', async () => {
    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', 'Bearer invalid-token-format');

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Invalid token');
    expect(response.body).toHaveProperty(
      'message',
      'Token is malformed or expired'
    );
  });

  // Integration test: Expired token
  // Input: Expired JWT token
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, token expired error
  test('Returns 401 when token is expired', async () => {
    const expiredToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '-1h',
    });

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${expiredToken}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Token expired');
    expect(response.body).toHaveProperty('message', 'Please login again');
  });

  // Integration test: Token for non-existent user
  // Input: Valid JWT token but user deleted
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, user not found error
  test('Returns 401 when user no longer exists', async () => {
    // Create and delete a user
    const tempUser = await userModel.create({
      email: 'temp-auth-test@test.com',
      name: 'Temp User',
      googleId: 'google-temp-auth-' + Date.now(),
    });

    const tempToken = jwt.sign(
      { id: tempUser._id.toString() },
      process.env.JWT_SECRET!,
      { expiresIn: '1h' }
    );

    // Delete the user
    await userModel.delete(tempUser._id);

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${tempToken}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'User not found');
    expect(response.body).toHaveProperty(
      'message',
      'Token is valid but user no longer exists'
    );
  });

  // Integration test: Token with invalid signature
  // Input: JWT token signed with wrong secret
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, invalid token error
  test('Returns 401 when token has invalid signature', async () => {
    const invalidToken = jwt.sign({ id: testUserId }, 'wrong-secret', {
      expiresIn: '1h',
    });

    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer ${invalidToken}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Invalid token');
  });

  // Integration test: Empty Bearer token
  // Input: Authorization header with "Bearer " but no token
  // Expected behavior: Returns 401 error
  // Expected output: 401 status, no token error
  test('Returns 401 when Bearer token is empty', async () => {
    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', 'Bearer ');

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Access denied');
  });

  // Integration test: Multiple spaces in Authorization header
  // Input: Authorization header with extra spaces
  // Expected behavior: Returns 401 error (token extraction fails)
  // Expected output: 401 status, no token error
  test('Returns 401 when Authorization header has extra spaces', async () => {
    const response = await request(app)
      .get('/test-auth')
      .set('Authorization', `Bearer  ${validToken}`);

    expect(response.status).toBe(401);
    expect(response.body).toHaveProperty('error', 'Access denied');
  });
});
