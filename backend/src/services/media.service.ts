import fs from 'fs';
import path from 'path';

import { IMAGES_DIR } from '../types/user.types';

export class MediaService {
  static async saveImage(filePath: string, userId: string): Promise<string> {
    try {
      const fileExtension = path.extname(filePath);
      const fileName = `${userId}-${Date.now()}${fileExtension}`;
      const newPath = path.join(IMAGES_DIR, fileName);

      fs.renameSync(filePath, newPath);

      // Return the web-accessible path from env variable
      const uploadsPath = process.env.UPLOADS_DIR || 'uploads/images';
      return `${uploadsPath}/${fileName}`;
    } catch (error) {
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
      throw new Error(`Failed to save profile picture: ${error}`);
    }
  }

  static async deleteImage(url: string): Promise<void> {
    try {
      const uploadsPath = process.env.UPLOADS_DIR || 'uploads/images';
      // url comes as "uploads/images/filename.jpg" from the database
      if (url.startsWith(uploadsPath)) {
        const fileName = path.basename(url);

        // Construct path without using user input directly in path.join
        const allowedDir = path.resolve(IMAGES_DIR);
        const filePath = `${allowedDir}/${fileName}`;
        const resolvedPath = path.resolve(filePath);

        // Security check: Ensure the resolved path is still within the allowed directory
        if (!resolvedPath.startsWith(allowedDir)) {
          throw new Error('Invalid file path - path traversal detected');
        }

        if (fs.existsSync(resolvedPath)) {
          fs.unlinkSync(resolvedPath);
        }
      }
    } catch (error) {
      console.error('Failed to delete old profile picture:', error);
    }
  }

  static async deleteAllUserImages(userId: string): Promise<void> {
    try {
      if (!fs.existsSync(IMAGES_DIR)) {
        return;
      }

      const files = fs.readdirSync(IMAGES_DIR);
      const userFiles = files.filter(file => file.startsWith(userId + '-'));

      const uploadsPath = process.env.UPLOADS_DIR || 'uploads/images';
      // Pass the full database path format to deleteImage
      await Promise.all(
        userFiles.map(file => this.deleteImage(`${uploadsPath}/${file}`))
      );
    } catch (error) {
      console.error('Failed to delete user images:', error);
    }
  }
}
