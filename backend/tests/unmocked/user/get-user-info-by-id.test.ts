import { describe, expect, test, beforeAll, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import router from '../../../src/routes/routes';
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
app.use('/api', router);

// Interface GET /api/user/:id (Integration)
describe('Unmocked GET /api/user/:id', () => {
  let authToken: string;
  let testUserId: string;
  let targetUserId: string;
  let targetUserName: string;
  let targetUserFriendCode: string;

  beforeAll(async () => {
    await connectDB();

    // Create test user (requester)
    const testUser = await userModel.create({
      email: 'requester-userinfo@test.com',
      name: 'Requester User',
      googleId: 'google-requester-userinfo',
    });
    testUserId = testUser._id.toString();

    // Create target user to fetch info about
    const targetUser = await userModel.create({
      email: 'target-userinfo@test.com',
      name: 'Target User',
      googleId: 'google-target-userinfo',
      bio: 'This is the target user bio',
      profilePicture: 'https://example.com/target.jpg',
    } as any);
    targetUserId = targetUser._id.toString();
    targetUserName = targetUser.name;
    targetUserFriendCode = targetUser.friendCode;

    // Generate auth token
    authToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });
  });

  afterAll(async () => {
    // Cleanup test users
    await userModel.delete(new mongoose.Types.ObjectId(testUserId));
    await userModel.delete(new mongoose.Types.ObjectId(targetUserId));
    await mongoose.connection.close();
  });

  // Integration test: Successfully get user info by ID
  // Input: Valid user ID
  // Expected behavior: Returns public user info from database
  // Expected output: 200 status, user info object with public fields only
  test('Successfully retrieves user info by ID from database', async () => {
    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'User info fetched successfully'
    );
    expect(response.body.data).toHaveProperty('userInfo');
    expect(response.body.data.userInfo).toHaveProperty('_id', targetUserId);
    expect(response.body.data.userInfo).toHaveProperty('name', targetUserName);
    expect(response.body.data.userInfo).toHaveProperty(
      'friendCode',
      targetUserFriendCode
    );
    expect(response.body.data.userInfo).toHaveProperty(
      'bio',
      'This is the target user bio'
    );
    expect(response.body.data.userInfo).toHaveProperty('profilePicture');
  });

  // Integration test: User info does not include private fields
  // Input: Valid user ID
  // Expected behavior: Returns only public fields, excludes email and googleId
  // Expected output: 200 status, user info without email and googleId
  test('Returns only public user info without private fields', async () => {
    const response = await request(app)
      .get(`/api/user/${targetUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data.userInfo).not.toHaveProperty('email');
    expect(response.body.data.userInfo).not.toHaveProperty('googleId');
  });

  // Integration test: Get user info with non-existent ID
  // Input: Non-existent user ID
  // Expected behavior: Returns 404 not found
  // Expected output: 404 status, error message
  test('Returns 404 when user ID does not exist in database', async () => {
    const nonExistentId = new mongoose.Types.ObjectId().toString();

    const response = await request(app)
      .get(`/api/user/${nonExistentId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('message', 'User info not found');
  });

  // Integration test: Get user info with invalid ID format
  // Input: Invalid ObjectId format
  // Expected behavior: Returns 500 error
  // Expected output: 500 status, error message
  test('Returns 500 when user ID format is invalid', async () => {
    const response = await request(app)
      .get('/api/user/invalid-id-format')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty('message');
  });

  // Integration test: Get user info without authentication
  // Input: No auth token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when not authenticated', async () => {
    const response = await request(app).get(`/api/user/${targetUserId}`);

    expect(response.status).toBe(401);
  });

  // Integration test: Get own user info by ID
  // Input: Requester's own user ID
  // Expected behavior: Returns own public user info
  // Expected output: 200 status, own user info
  test('Successfully retrieves own user info when requesting self', async () => {
    const response = await request(app)
      .get(`/api/user/${testUserId}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data.userInfo).toHaveProperty('_id', testUserId);
    expect(response.body.data.userInfo).toHaveProperty(
      'name',
      'Requester User'
    );
  });

  // Integration test: Get user info for user without optional fields
  // Input: User ID for user without bio and profile picture
  // Expected behavior: Returns user info with undefined optional fields
  // Expected output: 200 status, user info without optional fields
  test('Returns user info for user without optional fields', async () => {
    // Create user without optional fields
    const minimalUser = await userModel.create({
      email: 'minimal-userinfo@test.com',
      name: 'Minimal User',
      googleId: 'google-minimal-userinfo',
    });

    const response = await request(app)
      .get(`/api/user/${minimalUser._id.toString()}`)
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data.userInfo).toHaveProperty('name', 'Minimal User');
    expect(response.body.data.userInfo).toHaveProperty('friendCode');
    expect(response.body.data.userInfo.bio).toBeUndefined();
    expect(response.body.data.userInfo.profilePicture).toBeUndefined();

    // Cleanup
    await userModel.delete(minimalUser._id);
  });
});
