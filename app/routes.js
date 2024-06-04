import express from 'express';
import {
  login,
  register,
  logout,
  forgetPassword,
} from './handler.js';
import { authMiddleware } from './middleware.js';

const router = express.Router();

// Register
router.post('/register', register);

// Login
router.post('/login', login);

// Forget Password
router.post('/password-reset', forgetPassword);

// Logout
router.post('/logout', authMiddleware, logout);

export default router;
