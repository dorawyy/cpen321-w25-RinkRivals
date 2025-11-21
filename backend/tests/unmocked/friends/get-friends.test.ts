import {
  describe,
  expect,
  test,
  beforeAll,
  afterAll,
  afterEach,
  beforeEach,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import router from '../../../src/routes/routes';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { userModel } from '../../../src/models/user.model';
import { FriendRequest } from '../../../src/models/friends.model';
import { connectDB } from '../../../src/config/database';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface GET /api/friends/list (Integration)
describe('Unmocked GET /api/friends/list', () => {
  let authToken: string;
  let testUserId: string;
  let friend1Id: string;
  let friend2Id: string;
  let friend3Id: string;

  beforeAll(async () => {
    await connectDB();

    // Create test user
    const testUser = await userModel.create({
      email: 'user-friends@test.com',
      name: 'Test User',
      googleId: 'google-user-friends',
    });
    testUserId = testUser._id.toString();

    // Create friend 1
    const friend1 = await userModel.create({
      email: 'friend1@test.com',
      name: 'Friend One',
      googleId: 'google-friend-1',
    });
    friend1Id = friend1._id.toString();

    // Create friend 2
    const friend2 = await userModel.create({
      email: 'friend2@test.com',
      name: 'Friend Two',
      googleId: 'google-friend-2',
    });
    friend2Id = friend2._id.toString();

    // Create a pending request (should not appear in friends list)
    const friend3 = await userModel.create({
      email: 'pending-getfriends@test.com',
      name: 'Pending User',
      googleId: 'google-pending-getfriends',
    });
    friend3Id = friend3._id.toString();

    // Generate auth token
    authToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });
  });

  beforeEach(async () => {
    // Recreate friend requests before each test
    await FriendRequest.create({
      sender: testUserId,
      receiver: friend1Id,
      status: 'accepted',
    });

    await FriendRequest.create({
      sender: friend2Id,
      receiver: testUserId,
      status: 'accepted',
    });

    await FriendRequest.create({
      sender: testUserId,
      receiver: friend3Id,
      status: 'pending',
    });
  });

  afterEach(async () => {
    // Clean up friend requests after each test
    await FriendRequest.deleteMany({});
  });

  afterAll(async () => {
    // Cleanup all users
    await userModel.delete(new mongoose.Types.ObjectId(testUserId));
    await userModel.delete(new mongoose.Types.ObjectId(friend1Id));
    await userModel.delete(new mongoose.Types.ObjectId(friend2Id));
    await userModel.delete(new mongoose.Types.ObjectId(friend3Id));
    await mongoose.connection.close();
  });

  // Integration test: Successfully get friends list
  // Input: Authenticated user
  // Expected behavior: Returns list of accepted friend requests from database
  // Expected output: 200 status, array of 2 friends
  test('Successfully retrieves friends list from database', async () => {
    const response = await request(app)
      .get('/api/friends/list')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'Friends fetched successfully'
    );
    expect(response.body.data).toBeInstanceOf(Array);
    expect(response.body.data).toHaveLength(2);

    // Verify both friends are in the list
    const friendIds = response.body.data.map((fr: any) => {
      // Could be sender or receiver
      return fr.sender._id === testUserId ? fr.receiver._id : fr.sender._id;
    });
    expect(friendIds).toContain(friend1Id);
    expect(friendIds).toContain(friend2Id);
  });

  // Integration test: Friends have populated user data
  // Input: Authenticated user
  // Expected behavior: Returns friends with populated name and email
  // Expected output: Friend objects with user details
  test('Returns friends with populated user information', async () => {
    const response = await request(app)
      .get('/api/friends/list')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data).toBeInstanceOf(Array);

    // Check that sender and receiver are populated
    response.body.data.forEach((friendRequest: any) => {
      expect(friendRequest.sender).toHaveProperty('name');
      expect(friendRequest.sender).toHaveProperty('email');
      expect(friendRequest.receiver).toHaveProperty('name');
      expect(friendRequest.receiver).toHaveProperty('email');
    });
  });

  // Integration test: Get friends without authentication
  // Input: No auth token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when not authenticated', async () => {
    const response = await request(app).get('/api/friends/list');

    expect(response.status).toBe(401);
  });

  // Integration test: User with no friends
  // Input: New user with no friends
  // Expected behavior: Returns empty array
  // Expected output: 200 status, empty array
  test('Returns empty array when user has no friends', async () => {
    // Create new user with no friends
    const newUser = await userModel.create({
      email: 'nofriends@test.com',
      name: 'No Friends User',
      googleId: 'google-nofriends',
    });

    const noFriendsToken = jwt.sign(
      { id: newUser._id.toString() },
      process.env.JWT_SECRET!,
      { expiresIn: '1h' }
    );

    const response = await request(app)
      .get('/api/friends/list')
      .set('Authorization', `Bearer ${noFriendsToken}`);

    expect(response.status).toBe(200);
    expect(response.body.data).toBeInstanceOf(Array);
    expect(response.body.data).toHaveLength(0);

    // Cleanup
    await userModel.delete(newUser._id);
  });
});
