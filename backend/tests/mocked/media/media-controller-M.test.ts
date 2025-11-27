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
import { MediaService } from '../../../src/services/media.service';
import fs from 'fs';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api', router);

describe('Mocked POST /api/media/upload - Error Handling', () => {
  let authToken: string;
  let testUserId: string;
  let testImagePath: string;

  beforeAll(() => {
    jest.clearAllMocks();
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
      } as any;
    });

    // Create a test image
    const testDir = path.join(__dirname, '../../res');
    if (!fs.existsSync(testDir)) {
      fs.mkdirSync(testDir, { recursive: true });
    }
    testImagePath = path.join(testDir, 'test-mock-upload.jpg');
    fs.writeFileSync(testImagePath, Buffer.from('fake-image-data'));
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    jest.restoreAllMocks();
    // Clean up test image
    if (fs.existsSync(testImagePath)) {
      fs.unlinkSync(testImagePath);
    }
  });

  // Test: MediaService.saveImage throws Error instance (covers line 36-41)
  // Input: Valid file upload, saveImage fails with Error
  // Expected behavior: Returns 500 with error message
  // Expected output: 500 status with error message
  test('Returns 500 when MediaService.saveImage throws Error', async () => {
    jest
      .spyOn(MediaService, 'saveImage')
      .mockRejectedValueOnce(new Error('Disk write failed'));

    const response = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${authToken}`)
      .attach('media', testImagePath);

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty('message', 'Disk write failed');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  // Test: MediaService.saveImage throws Error with empty message (covers line 38-41)
  // Input: Valid file upload, saveImage fails with Error('')
  // Expected behavior: Returns 500 with fallback message
  // Expected output: 500 status with default message
  test('Returns default message when error message is empty', async () => {
    jest.spyOn(MediaService, 'saveImage').mockRejectedValueOnce(new Error(''));

    const response = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${authToken}`)
      .attach('media', testImagePath);

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty(
      'message',
      'Failed to upload profile picture'
    );
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  // Test: MediaService.saveImage throws non-Error exception (covers line 45-47)
  // Input: Valid file upload, saveImage fails with non-Error
  // Expected behavior: Passes to next() error handler
  // Expected output: Error handled by error middleware
  test('Handles non-Error exception from MediaService.saveImage', async () => {
    jest
      .spyOn(MediaService, 'saveImage')
      .mockRejectedValueOnce('String error' as any);

    const response = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${authToken}`)
      .attach('media', testImagePath);

    expect(response.status).toBe(500);
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  // Test: CRLF injection attempt in file path (covers sanitizeInput.util.ts line 7)
  // Input: File path with newline character
  // Expected behavior: Throws CRLF injection error
  // Expected output: 500 status with CRLF error message
  test('Detects CRLF injection attempt in file path', async () => {
    // Spy on sanitizeInput and mock it to call the original with a malicious path
    const sanitizeUtil = require('../../../src/utils/sanitizeInput.util');
    const originalSanitize = sanitizeUtil.sanitizeInput;
    
    const mockSanitize = jest.spyOn(sanitizeUtil, 'sanitizeInput');
    mockSanitize.mockImplementationOnce(() => {
      // Call original with malicious input to trigger the CRLF detection
      return originalSanitize('test\npath.jpg');
    });

    const response = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${authToken}`)
      .attach('media', testImagePath);

    expect(response.status).toBe(500);
    expect(response.body.message).toContain('CRLF injection');
    
    mockSanitize.mockRestore();
  });
});
