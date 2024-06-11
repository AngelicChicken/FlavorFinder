import express from "express";
import {
  login,
  register,
  logout,
  forgetPassword,
  createBookmark,
  deleteBookmark,
  getBookmark,
  getUser,
  updateUser,
} from "./handler.js";
import authMiddleware from "./middleware.js";
import multer from "multer";

const router = express.Router();
const storage = multer.memoryStorage();
const upload = multer({ storage: storage });

// Register
router.post("/register", register);

// Login
router.post("/login", login);

// Forget Password
router.post("/password-reset", forgetPassword);

// Logout
router.post("/logout", authMiddleware, logout);

// Create Bookmark
router.post("/bookmark", authMiddleware, createBookmark);

// Delete Bookmark
router.delete("/bookmark/:id", authMiddleware, deleteBookmark);

// Get Bookmark
router.get("/bookmark", authMiddleware, getBookmark);

// Get user profile
router.get("/user/:id", authMiddleware, getUser);

// Update user profile
router.put("/user/:id", authMiddleware, upload.single("profileImage"), updateUser);

export default router;
