import {
  describe,
  expect,
  test,
  jest,
  beforeAll,
  afterAll,
} from '@jest/globals';
import dotenv from 'dotenv';
import request from 'supertest';
import express, { Request, Response, NextFunction } from 'express';
import router from '../../src/routes/routes';
import {
  notFoundHandler,
  errorHandler,
} from '../../src/middleware/errorHandler.middleware';
import path from 'path';

// Load test environment variables
dotenv.config({ path: path.resolve(__dirname, '../../.env.test') });

// Create Express app for testing
const app = express();
app.use(express.json());

// Add a test route that throws an error
app.get('/test-error', (req: Request, res: Response, next: NextFunction) => {
  const error = new Error('Test error thrown');
  next(error);
});

app.use('/api', router);

// Apply error handlers
app.use(notFoundHandler);
app.use((error: Error, req: Request, res: Response, next: NextFunction) =>
  errorHandler(error, req, res)
);

/**
 * Integration Tests for Error Handler Middleware
 *
 * These tests verify the error handling middleware functions:
 * - notFoundHandler: Handles 404 errors for non-existent routes
 * - errorHandler: Handles 500 errors for application errors
 */

describe('Error Handler Middleware Integration Tests', () => {
  beforeAll(() => {
    // Silence console.error during tests
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // Input: GET request to non-existent route
  // Expected behavior: notFoundHandler catches and returns 404
  // Expected output: 404 status with error details
  test('notFoundHandler returns 404 for non-existent routes', async () => {
    const response = await request(app).get('/api/non-existent-route');

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('error', 'Route not found');
    expect(response.body).toHaveProperty(
      'message',
      'Cannot GET /api/non-existent-route'
    );
    expect(response.body).toHaveProperty('timestamp');
    expect(response.body).toHaveProperty('path', '/api/non-existent-route');
    expect(response.body).toHaveProperty('method', 'GET');
  });

  // Input: POST request to non-existent route
  // Expected behavior: notFoundHandler catches and returns 404 with POST method
  // Expected output: 404 status with POST method in response
  test('notFoundHandler handles POST to non-existent routes', async () => {
    const response = await request(app)
      .post('/api/does-not-exist')
      .send({ data: 'test' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('error', 'Route not found');
    expect(response.body).toHaveProperty(
      'message',
      'Cannot POST /api/does-not-exist'
    );
    expect(response.body).toHaveProperty('method', 'POST');
  });

  // Input: DELETE request to non-existent route
  // Expected behavior: notFoundHandler catches and returns 404 with DELETE method
  // Expected output: 404 status with DELETE method in response
  test('notFoundHandler handles DELETE to non-existent routes', async () => {
    const response = await request(app).delete('/api/missing-endpoint');

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('error', 'Route not found');
    expect(response.body).toHaveProperty(
      'message',
      'Cannot DELETE /api/missing-endpoint'
    );
    expect(response.body).toHaveProperty('method', 'DELETE');
  });

  // Input: Route that throws an error
  // Expected behavior: errorHandler catches and returns 500
  // Expected output: 500 status with internal server error message
  test('errorHandler handles thrown errors', async () => {
    const response = await request(app).get('/test-error');

    expect(response.status).toBe(500);
    expect(response.body).toHaveProperty('message', 'Internal server error');
  });

  // Input: Non-existent nested route
  // Expected behavior: notFoundHandler returns 404 with full path
  // Expected output: 404 with nested path in response
  test('notFoundHandler handles nested non-existent routes', async () => {
    const response = await request(app).get(
      '/api/auth/nested/route/that/does/not/exist'
    );

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('error', 'Route not found');
    expect(response.body).toHaveProperty(
      'path',
      '/api/auth/nested/route/that/does/not/exist'
    );
    expect(response.body).toHaveProperty('timestamp');
  });

  // Input: PUT request to non-existent route
  // Expected behavior: notFoundHandler handles PUT method
  // Expected output: 404 with PUT method
  test('notFoundHandler handles PUT to non-existent routes', async () => {
    const response = await request(app)
      .put('/api/update-nothing')
      .send({ update: 'data' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('method', 'PUT');
    expect(response.body).toHaveProperty(
      'message',
      'Cannot PUT /api/update-nothing'
    );
  });

  // Input: PATCH request to non-existent route
  // Expected behavior: notFoundHandler handles PATCH method
  // Expected output: 404 with PATCH method
  test('notFoundHandler handles PATCH to non-existent routes', async () => {
    const response = await request(app)
      .patch('/api/patch-endpoint')
      .send({ patch: 'data' });

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty('method', 'PATCH');
  });

  // Input: Request with query parameters to non-existent route
  // Expected behavior: notFoundHandler includes query params in path
  // Expected output: 404 with full path including query string
  test('notFoundHandler includes query parameters in path', async () => {
    const response = await request(app).get('/api/search?query=test&limit=10');

    expect(response.status).toBe(404);
    expect(response.body).toHaveProperty(
      'path',
      '/api/search?query=test&limit=10'
    );
  });

  // Input: Timestamp validation
  // Expected behavior: Timestamp is valid ISO string
  // Expected output: Valid ISO 8601 timestamp
  test('notFoundHandler returns valid ISO timestamp', async () => {
    const response = await request(app).get('/api/test-timestamp');

    expect(response.status).toBe(404);
    expect(response.body.timestamp).toBeDefined();

    // Validate it's a valid ISO string
    const timestamp = new Date(response.body.timestamp);
    expect(timestamp).toBeInstanceOf(Date);
    expect(timestamp.toISOString()).toBe(response.body.timestamp);
  });
});
