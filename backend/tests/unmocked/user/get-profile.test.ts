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
});
