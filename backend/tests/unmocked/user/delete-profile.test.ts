import {
  describe,
  expect,
  test,
  beforeAll,
  afterAll,
  beforeEach,
  afterEach,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express from 'express';
import router from '../../../src/routes/routes';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { userModel } from '../../../src/models/user.model';
import { connectDB } from '../../../src/config/database';
import { FriendRequest } from '../../../src/models/friends.model';
import path from 'path';
import fs from 'fs';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

// Interface DELETE /api/user/profile (Integration)
describe('Unmocked DELETE /api/user/profile', () => {
  let authToken: string;
  let testUserId: string;

  beforeEach(async () => {
    await connectDB();

    // Create fresh test user before each test
    const testUser = await userModel.create({
      email: 'delete-profile@test.com',
      name: 'Delete Test User',
      googleId: `google-delete-${Date.now()}`,
    });
    testUserId = testUser._id.toString();

    // Generate auth token
    authToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '1h',
    });
  });

  afterEach(async () => {
    // Cleanup: try to delete test user if it still exists
    try {
      const user = await userModel.findById(
        new mongoose.Types.ObjectId(testUserId)
      );
      if (user) {
        await userModel.delete(new mongoose.Types.ObjectId(testUserId));
      }
    } catch (error) {
      // User already deleted, which is fine
    }

    // Clean up any friend requests
    await FriendRequest.deleteMany({
      $or: [{ sender: testUserId }, { receiver: testUserId }],
    });
  });

  afterAll(async () => {
    await mongoose.connection.close();
  });

  // Integration test: Successfully delete user profile
  // Input: Authenticated user
  // Expected behavior: Removes user from database
  // Expected output: 200 status, success message
  test('Successfully deletes user profile from database', async () => {
    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'User deleted successfully'
    );

    // Verify user is removed from database
    const deletedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(deletedUser).toBeNull();
  });

  // Integration test: Delete profile without authentication
  // Input: No auth token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when not authenticated', async () => {
    const response = await request(app).delete('/api/user/profile');

    expect(response.status).toBe(401);

    // Verify user still exists
    const user = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(user).not.toBeNull();
  });

  // Integration test: Delete profile with invalid token
  // Input: Invalid JWT token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when token is invalid', async () => {
    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', 'Bearer invalid-token-12345');

    expect(response.status).toBe(401);

    // Verify user still exists
    const user = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(user).not.toBeNull();
  });

  // Integration test: Cannot access deleted user profile
  // Input: Try to get profile after deletion
  // Expected behavior: Returns 401 (token still valid but user doesn't exist)
  // Expected output: 401 or error status
  test('Cannot access profile after deletion', async () => {
    // Delete the user
    await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    // Try to access profile with same token
    const response = await request(app)
      .get('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    // Should fail because user no longer exists even though token is valid
    expect(response.status).not.toBe(200);
  });

  // Integration test: Delete user with friend requests
  // Input: User with pending friend requests
  // Expected behavior: Deletes user and cleans up friend requests
  // Expected output: 200 status, user and related data removed
  test('Successfully deletes user with associated friend requests', async () => {
    // Create another user to send friend request
    const otherUser = await userModel.create({
      email: 'other-user@test.com',
      name: 'Other User',
      googleId: 'google-other-user',
    });

    // Create a friend request
    await FriendRequest.create({
      sender: testUserId,
      receiver: otherUser._id.toString(),
      status: 'pending',
    });

    // Delete the test user
    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);

    // Verify user is deleted
    const deletedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(deletedUser).toBeNull();

    // Note: Friend requests cleanup depends on implementation
    // The test mainly verifies the user deletion succeeds even with friend requests

    // Cleanup
    await userModel.delete(otherUser._id);
    await FriendRequest.deleteMany({
      $or: [
        { sender: testUserId },
        { receiver: testUserId },
        { sender: otherUser._id.toString() },
        { receiver: otherUser._id.toString() },
      ],
    });
  });

  // Integration test: Delete profile with expired token
  // Input: Expired JWT token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when token is expired', async () => {
    const expiredToken = jwt.sign({ id: testUserId }, process.env.JWT_SECRET!, {
      expiresIn: '-1h',
    });

    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${expiredToken}`);

    expect(response.status).toBe(401);

    // Verify user still exists
    const user = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(user).not.toBeNull();
  });

  // Integration test: Multiple delete attempts on same user
  // Input: Try to delete already deleted user
  // Expected behavior: First delete succeeds, second returns error
  // Expected output: First 200, second 401 or error
  test('Cannot delete user that is already deleted', async () => {
    // First deletion
    const firstResponse = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(firstResponse.status).toBe(200);

    // Second deletion attempt with same token
    const secondResponse = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    // Should fail because user no longer exists
    expect(secondResponse.status).not.toBe(200);
  });

  // Integration test: Delete removes user from all queries
  // Input: Delete user, then try to find by ID and friend code
  // Expected behavior: User not found in any query
  // Expected output: 200 delete status, null on subsequent queries
  test('Deleted user is not found in any queries', async () => {
    // Get friend code before deletion
    const userBeforeDelete = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    const friendCode = userBeforeDelete!.friendCode;

    // Delete the user
    const response = await request(app)
      .delete('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`);

    expect(response.status).toBe(200);

    // Try to find by ID
    const userById = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(userById).toBeNull();

    // Try to find by friend code
    const userByCode = await userModel.findByFriendCode(friendCode);
    expect(userByCode).toBeNull();
  });

  // Model test: userModel.delete handles non-existent user without error
  // Input: Non-existent user ID
  // Expected behavior: Completes without error
  // Expected output: void (no error thrown)
  test('userModel.delete handles non-existent user without error', async () => {
    const fakeId = new mongoose.Types.ObjectId();

    await expect(userModel.delete(fakeId)).resolves.toBeUndefined();
  });
});
