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

// Interface PUT /api/user/profile (Integration)
describe('Unmocked PUT /api/user/profile', () => {
  let authToken: string;
  let testUserId: string;

  beforeAll(async () => {
    await connectDB();

    // Create test user
    const testUser = await userModel.create({
      email: 'update-profile@test.com',
      name: 'Original Name',
      googleId: 'google-update-profile',
    });
    testUserId = testUser._id.toString();

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

  // Integration test: Successfully update profile name
  // Input: New name in request body
  // Expected behavior: Updates user name in database
  // Expected output: 200 status, updated user profile
  test('Successfully updates user name in database', async () => {
    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Updated Name' });

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty(
      'message',
      'User info updated successfully'
    );
    expect(response.body.data).toHaveProperty('user');
    expect(response.body.data.user).toHaveProperty('name', 'Updated Name');
    expect(response.body.data.user).toHaveProperty('_id', testUserId);

    // Verify in database
    const updatedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(updatedUser).not.toBeNull();
    expect(updatedUser!.name).toBe('Updated Name');
  });

  // Integration test: Successfully update profile bio
  // Input: New bio in request body
  // Expected behavior: Updates user bio in database
  // Expected output: 200 status, updated user profile with bio
  test('Successfully updates user bio in database', async () => {
    const newBio = 'This is my updated bio';

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ bio: newBio });

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('bio', newBio);

    // Verify in database
    const updatedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(updatedUser).not.toBeNull();
    expect(updatedUser!.bio).toBe(newBio);
  });

  // Integration test: Successfully update profile picture
  // Input: New profile picture URL in request body
  // Expected behavior: Updates user profile picture in database
  // Expected output: 200 status, updated user profile with new picture
  test('Successfully updates user profile picture in database', async () => {
    const newPicture = 'https://example.com/new-profile.jpg';

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ profilePicture: newPicture });

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty(
      'profilePicture',
      newPicture
    );

    // Verify in database
    const updatedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(updatedUser).not.toBeNull();
    expect(updatedUser!.profilePicture).toBe(newPicture);
  });

  // Integration test: Successfully update multiple fields
  // Input: Multiple fields in request body (name, bio, profilePicture)
  // Expected behavior: Updates all provided fields in database
  // Expected output: 200 status, updated user profile with all changes
  test('Successfully updates multiple profile fields in database', async () => {
    const updates = {
      name: 'Multi Updated Name',
      bio: 'Multi updated bio',
      profilePicture: 'https://example.com/multi-update.jpg',
    };

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send(updates);

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('name', updates.name);
    expect(response.body.data.user).toHaveProperty('bio', updates.bio);
    expect(response.body.data.user).toHaveProperty(
      'profilePicture',
      updates.profilePicture
    );

    // Verify in database
    const updatedUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );
    expect(updatedUser).not.toBeNull();
    expect(updatedUser!.name).toBe(updates.name);
    expect(updatedUser!.bio).toBe(updates.bio);
    expect(updatedUser!.profilePicture).toBe(updates.profilePicture);
  });

  // Integration test: Update with empty request body
  // Input: Empty request body
  // Expected behavior: Returns user unchanged
  // Expected output: 200 status, user profile unchanged
  test('Returns unchanged profile when updating with empty body', async () => {
    // First, get current user state
    const currentUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({});

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('name', currentUser!.name);
  });

  // Integration test: Update profile without authentication
  // Input: No auth token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when not authenticated', async () => {
    const response = await request(app)
      .put('/api/user/profile')
      .send({ name: 'Unauthorized Update' });

    expect(response.status).toBe(401);
  });

  // Integration test: Update with invalid name (empty string)
  // Input: Empty name string
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when name is empty string', async () => {
    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: '' });

    expect(response.status).toBe(400);
  });

  // Integration test: Update with invalid bio (exceeds max length)
  // Input: Bio exceeding 500 characters
  // Expected behavior: Returns 400 validation error
  // Expected output: 400 status, validation error message
  test('Returns 400 when bio exceeds max length', async () => {
    const longBio = 'a'.repeat(501);

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ bio: longBio });

    expect(response.status).toBe(400);
  });

  // Integration test: Update profile with invalid token
  // Input: Invalid JWT token
  // Expected behavior: Returns 401 unauthorized
  // Expected output: 401 status
  test('Returns 401 when token is invalid', async () => {
    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', 'Bearer invalid-token-12345')
      .send({ name: 'Invalid Token Update' });

    expect(response.status).toBe(401);
  });

  // Integration test: Update profile preserves unchanged fields
  // Input: Update only name
  // Expected behavior: Updates name, preserves other fields
  // Expected output: 200 status, name updated, other fields unchanged
  test('Preserves unchanged fields when updating single field', async () => {
    // Set up initial state
    await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({
        name: 'Preserve Test Name',
        bio: 'Preserve Test Bio',
        profilePicture: 'https://example.com/preserve.jpg',
      });

    // Update only name
    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({ name: 'Only Name Updated' });

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('name', 'Only Name Updated');
    expect(response.body.data.user).toHaveProperty('bio', 'Preserve Test Bio');
    expect(response.body.data.user).toHaveProperty(
      'profilePicture',
      'https://example.com/preserve.jpg'
    );
  });

  // Integration test: Update does not allow changing immutable fields
  // Input: Attempt to change email or googleId
  // Expected behavior: These fields are ignored, not updated
  // Expected output: 200 status, immutable fields unchanged
  test('Ignores attempts to update immutable fields', async () => {
    const originalUser = await userModel.findById(
      new mongoose.Types.ObjectId(testUserId)
    );

    const response = await request(app)
      .put('/api/user/profile')
      .set('Authorization', `Bearer ${authToken}`)
      .send({
        name: 'Valid Update',
        email: 'newemail@test.com',
        googleId: 'new-google-id',
      } as any);

    expect(response.status).toBe(200);
    expect(response.body.data.user).toHaveProperty('name', 'Valid Update');
    expect(response.body.data.user).toHaveProperty(
      'email',
      originalUser!.email
    );
    expect(response.body.data.user).toHaveProperty(
      'googleId',
      originalUser!.googleId
    );
  });
});
