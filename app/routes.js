import express from "express";
import {
  login,
  register,
  logout,
  forgetPassword,
  createBookmark,
  deleteBookmark,
  getBookmark,
} from "./handler.js";
import { authMiddleware, verifyToken } from "./middleware.js";

const router = express.Router();

// Register
router.post("/register", register);

// Login
router.post("/login", login);

// Forget Password
router.post("/password-reset", forgetPassword);

// Logout
router.post("/logout", authMiddleware, logout);

// Create Bookmark
router.post("/bookmark", verifyToken, createBookmark);

// Delete Bookmark
router.delete("/bookmark/:id", verifyToken, deleteBookmark);

// Get Bookmark
router.get("/bookmark", verifyToken, getBookmark);

export default router;
