import mongoose, { Document } from 'mongoose';
import z from 'zod';
import path from 'path';

// Use environment variable for uploads directory with fallback
// This ensures images are saved in backend/uploads/images regardless of where the server runs from
export const IMAGES_DIR = path.join(
  process.cwd(),
  process.env.UPLOADS_DIR || 'uploads/images'
);

// User model
// ------------------------------------------------------------
export interface IUser extends Document {
  _id: mongoose.Types.ObjectId;
  googleId: string;
  email: string;
  name: string;
  profilePicture?: string;
  bio?: string;
  createdAt: Date;
  updatedAt: Date;
  friendCode: string;
}

// Zod schemas
// ------------------------------------------------------------
export const createUserSchema = z.object({
  email: z.string().email(),
  name: z.string().min(1),
  googleId: z.string().min(1),
  profilePicture: z.string().optional(),
  bio: z.string().max(500).optional(),
  friendCode: z.string().optional(),
});

export const updateProfileSchema = z.object({
  name: z.string().min(1).optional(),
  bio: z.string().max(500).optional(),
  profilePicture: z.string().min(1).optional(),
});

// Request types
// ------------------------------------------------------------
export type GetProfileResponse = {
  message: string;
  data?: {
    user: IUser;
  };
};

export type UpdateProfileRequest = z.infer<typeof updateProfileSchema>;

// Generic types
// ------------------------------------------------------------
export type GoogleUserInfo = {
  googleId: string;
  email: string;
  name: string;
  profilePicture?: string;
};

export type PublicUserInfo = {
  _id: mongoose.Types.ObjectId;
  name: string;
  profilePicture?: string;
  bio?: string;
  friendCode: string;
};
