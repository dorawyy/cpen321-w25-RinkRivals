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

// Interface GET /api/user/profile (Integration)
describe('Unmocked GET /api/user/profile', () => {
  let authToken: string;
  let testUserId: string;
  let testUserEmail: string;
  let testUserName: string;
  let testUserBio: string;

  beforeAll(async () => {
    await connectDB();

    // Create test user with full profile data
    const testUser = await userModel.create({
      email: 'profile-test@test.com',
      name: 'Profile Test User',
      googleId: 'google-profile-test',
      bio: 'This is my test bio',
      profilePicture: 'https://example.com/profile.jpg',
    } as any);
    testUserId = testUser._id.toString();
    testUserEmail = testUser.email;
    testUserName = testUser.name;
    testUserBio = testUser.bio!;

    // Generate auth token
    authToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });
  });

  afterAll(async () => {
    // Cleanup test user
    await userModel.delete(new mongoose.Types.ObjectId(testUserId));
    await mongoose.connection.close();
  });

  // Integration test: Successfully get user profile
  // Input: Authenticated user
  // Expected behavior: Returns current user's profile from database
  // Expected output: 200 status, user profile object with all fields
  test('Successfully retrieves authenticated user profile from database', async () => {
    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'Profile fetched successfully'
    );
    expect(response.body.data).toHaveProperty('user');
    expect(response.body.data.user).toHaveProperty('_id', testUserId);
    expect(response.body.data.user).toHaveProperty('email', testUserEmail);
    expect(response.body.data.user).toHaveProperty('name', testUserName);
    expect(response.body.data.user).toHaveProperty('bio', testUserBio);
    expect(response.body.data.user).toHaveProperty(
      'googleId',
      'google-profile-test'
    );
    expect(response.body.data.user).toHaveProperty('friendCode');
    expect(response.body.data.user).toHaveProperty('createdAt');
    expect(response.body.data.user).toHaveProperty('updatedAt');
  });

  // Integration test: Profile includes friend code
  // Input: Authenticated user
  // Expected behavior: Returns profile with unique friend code
  // Expected output: 200 status, profile with friendCode field
  test('Returns profile with friend code', async () => {
    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('friendCode');
    expect(typeof response.body.data.user.friendCode).toBe('string');
    expect(response.body.data.user.friendCode.length).toBeGreaterThan(0);
  });

  // Integration test: Get profile without authentication
  // Input: No auth token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when not authenticated', async () => {
    const response = await request(app).get('/api/user/profile');

    expect(response.status).toBe(401);
  });

  // Integration test: Get profile with invalid token
  // Input: Invalid JWT token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when token is invalid', async () => {
    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', 'Bearer invalid-token-12345');

    expect(response.status).toBe(401);
  });

  // Integration test: Get profile with expired token
  // Input: Expired JWT token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when token is expired', async () => {
    const expiredToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '-1h',
    });

    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${expiredToken}`);

    expect(response.status).toBe(401);
  });

  // Integration test: Profile for user without optional fields
  // Input: Authenticated user without bio and profile picture
  // Expected behavior: Returns profile with null/undefined optional fields
  // Expected output: 200 status, profile without optional fields
  test('Returns profile for user without optional fields', async () => {
    // Create user without optional fields
    const minimalUser = await userModel.create({
      email: 'minimal-profile@test.com',
      name: 'Minimal User',
      googleId: 'google-minimal-profile',
    });

    const minimalToken = jwt.sign(
      { id: minimalUser._id.toString() },
      process.env.JWT_SECRET!,
      { expiresIn: '1h' }
    );

    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${minimalToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('name', 'Minimal User');
    expect(response.body.data.user).toHaveProperty(
      'email',
      'minimal-profile@test.com'
    );
    expect(response.body.data.user.bio).toBeUndefined();
    expect(response.body.data.user.profilePicture).toBeUndefined();

    // Cleanup
    await userModel.delete(minimalUser._id);
  });

  // Model test: userModel.findByGoogleId returns null for non-existent googleId
  // Input: Non-existent Google ID
  // Expected behavior: Returns null
  // Expected output: null
  test('userModel.findByGoogleId returns null when not found', async () => {
    const result = await userModel.findByGoogleId('non-existent-google-id');

    expect(result).toBeNull();
  });

  // Model test: userModel.findByFriendCode returns null for non-existent code
  // Input: Non-existent friend code
  // Expected behavior: Returns null
  // Expected output: null
  test('userModel.findByFriendCode returns null when not found', async () => {
    const result = await userModel.findByFriendCode('NOTEXIST123');

    expect(result).toBeNull();
  });

  // Model test: userModel.create generates unique 6-character friend code
  // Input: Valid user data
  // Expected behavior: Creates user with generated friend code
  // Expected output: User with 6-character alphanumeric friend code
  test('userModel.create generates unique friend code', async () => {
    const testUser = {
      googleId: 'google-friendcode-test-' + Date.now(),
      email: `friendcode-test-${Date.now()}@test.com`,
      name: 'Friend Code Test User',
    };

    const user = await userModel.create(testUser);

    expect(user).toHaveProperty('friendCode');
    expect(user.friendCode).toHaveLength(6);
    expect(user.friendCode).toMatch(/^[A-Z0-9]+$/);

    // Cleanup
    await userModel.delete(user._id);
  });

  // Model test: userModel.findByGoogleId returns full user
  // Input: Existing Google ID
  // Expected behavior: Returns full user object
  // Expected output: User with all fields
  test('userModel.findByGoogleId returns full user object', async () => {
    const testUser = {
      googleId: 'google-findtest-' + Date.now(),
      email: `findtest-${Date.now()}@test.com`,
      name: 'Find Test User',
    };

    const createdUser = await userModel.create(testUser);
    const foundUser = await userModel.findByGoogleId(testUser.googleId);

    expect(foundUser).not.toBeNull();
    expect(foundUser?._id.toString()).toBe(createdUser._id.toString());
    expect(foundUser?.googleId).toBe(testUser.googleId);
    expect(foundUser?.email).toBe(testUser.email);

    // Cleanup
    await userModel.delete(createdUser._id);
  });

  // Model test: userModel.findById returns full user object
  // Input: Existing user ID
  // Expected behavior: Returns full user object
  // Expected output: User with all fields
  test('userModel.findById returns full user object', async () => {
    const testUser = {
      googleId: 'google-findbyid-' + Date.now(),
      email: `findbyid-${Date.now()}@test.com`,
      name: 'FindById Test User',
    };

    const createdUser = await userModel.create(testUser);
    const foundUser = await userModel.findById(createdUser._id);

    expect(foundUser).not.toBeNull();
    expect(foundUser?._id.toString()).toBe(createdUser._id.toString());
    expect(foundUser?.googleId).toBe(testUser.googleId);
    expect(foundUser?.email).toBe(testUser.email);
    expect(foundUser?.name).toBe(testUser.name);

    // Cleanup
    await userModel.delete(createdUser._id);
  });
});
