import admin from "firebase-admin";
import firebase from "firebase/compat/app";
import "firebase/compat/auth";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import { readFileSync } from "fs";
import db from "./config/firebase.config.js";
import { badResponse, successResponse } from "./response.js";
import { dateTimeNow } from "./time.js";
import axios from "axios";

const filename = fileURLToPath(import.meta.url);
const filedirname = dirname(filename);

const configPath = join(filedirname, "./config/config.json");
const config = JSON.parse(readFileSync(configPath));

const firebaseConfigPath = join(
  filedirname,
  "./config/",
  config.firebaseConfigCredentail
);
const firebaseConfig = JSON.parse(readFileSync(firebaseConfigPath, "utf8"));

firebase.initializeApp(firebaseConfig);

// Register 
const createUser = async (
  email,
  password,
  username,
  imgUrl = ""
) => {
  try {
    const requiredFields = ["email", "password", "username"];
    const missingFields = requiredFields.filter(
      () => !email || !password || !username
    );
    if (missingFields.length > 0) {
      const errorMessage = missingFields
        .map((field) => `${field} is required`)
        .join(". ");  
      const error = new Error(errorMessage);
      error.statusCode = 404;
      throw error;
    }

    // Mengecek format email apakah mengandung "@gmail.com"
    if (!email.includes("@gmail.com")) {
      const errorMessage = "Invalid Gmail Format";
      const error = new Error(errorMessage);
      error.statusCode = 404;
      throw error;
    }

    // Mengecek jika username unik
    const usernameSnapshot = await db
      .collection("users")
      .where("username", "==", username)
      .get();
    if (!usernameSnapshot.empty) {
      const errorMessage = `Username "${username}" is already taken`;
      const error = new Error(errorMessage);
      error.statusCode = 409;
      throw error;
    }

    const userCredential = await firebase
      .auth()
      .createUserWithEmailAndPassword(email, password);
    const userRecord = userCredential.user;

    const userDocRef = db.collection("users").doc(userRecord.uid);
    const userData = {
      user_id: userRecord.uid,
      email,
      password,
      username,
      imgUrl,
      email_verified: userRecord.emailVerified,
    };

    delete userData.password;
    await userDocRef.set(userData);

    await userRecord.sendEmailVerification();

    const userRecordResponse = {
      emailVerified: userRecord.emailVerified,
      isAnonymous: userRecord.isAnonymous,
      createAt: dateTimeNow(),
    };

    const responseData = { ...userData, userRecordResponse };
    return responseData;
  } catch (error) {
    console.error("Error creating user", error.message);
    throw error;
  }
};

const register = async (req, res) => {
  const { email, password, username, imgUrl } =
    req.body;

  try {
    const userResponse = await createUser(
      email,
      password,
      username,
      imgUrl
    );

    delete userResponse.password;

    const response = successResponse(
      201,
      "User Success Register, Check your email for verification",
      userResponse
    );
    res.status(201).json(response);
  } catch (error) {
    const statusCode = error.statusCode || 500;
    const response = badResponse(
      statusCode,
      "Error While Creating User",
      error.message
    );
    res.status(statusCode).json(response);
  }
};

// Login
const loginUser = async (email, password) => {
  try {
    const userCredential = await firebase
      .auth()
      .signInWithEmailAndPassword(email, password);

    // Generate JWT Token
    const token = await userCredential.user.getIdToken();
    const userRecord = await userCredential.user.getIdTokenResult();

    return {
      token,
      loginTime: dateTimeNow(),
      userRecord,
    };
  } catch (error) {
    console.error("Error logging in user:", error);
    throw error;
  }
};

const login = async (req, res) => {
  const { identifier, password } = req.body;

  try {
    if (identifier.length === 0) {
      const response = badResponse(400, "Email or username is required");
      return res.status(400).json(response);
    }

    if (password.length === 0) {
      const response = badResponse(400, "Password is required");
      return res.status(400).json(response);
    }

    // Mengecek apakah identifier merupakan email atau username
    let userSnapshot;
    if (identifier.includes("@")) {
      // Jika identifier mengandung karakter "@", maka dianggap sebagai email
      userSnapshot = await db
        .collection("users")
        .where("email", "==", identifier)
        .get();
    } else {
      // Jika tidak mengandung karakter "@", maka dianggap sebagai username
      userSnapshot = await db
        .collection("users")
        .where("username", "==", identifier)
        .get();
    }

    if (userSnapshot.empty) {
      // Jika tidak ada user dengan email atau username yang sesuai
      const response = badResponse(
        404,
        "User not found, please make sure your email format or username is correct"
      );
      return res.status(404).json(response);
    }

    const userData = userSnapshot.docs[0].data();
    const userRef = db.collection("users").doc(userData.user_id);

    const { token, userRecord } = await loginUser(userData.email, password);

    const verifiedData = await userRecord.claims.email_verified;
    if (verifiedData === true) {
      await userRef.update({ email_verified: true });
    }

    const responseData = {
      token,
      user: {
        ...userData,
      },
    };

    const response = successResponse(200, "User Success Login", responseData);
    return res.status(200).json(response);
  } catch (error) {
    let errorMessage = "";
    let status = null;

    if (error.code === "auth/invalid-credential") {
      status = 400;
      errorMessage = "Incorrect password";
      console.log(errorMessage);
    } else if (error.code === "auth/invalid-email") {
      status = 400;
      errorMessage = "Invalid email address";
    } else if (error.code === "auth/user-not-found") {
      status = 404;
      errorMessage = "User not found";
    } else {
      status = 500;
      errorMessage = `Error logging in user: ${error}`;
    }
    const response = badResponse(status, errorMessage);
    return res.status(status).json(response);
  }
};

// Logout
const logout = async (req, res) => {
  try {
    const { authorization } = req.headers;
    const { uid, email } = req.user;
    if (!authorization || !authorization.startsWith("Bearer ")) {
      throw new Error("Unauthorized");
    }

    const token = authorization.split("Bearer ")[1];

    await db.collection("tokens").doc(token).set({ invalid: true });

    await admin.auth().revokeRefreshTokens(uid);

    const logoutTime = dateTimeNow();

    // Tandai token sebagai tidak valid di Firestore
    await db.collection("tokens").doc(token).set({
      email,
      uid,
      invalid: true,
      time: logoutTime,
      type: "logout tokens",
      token,
    });

    const responseData = { email, time: logoutTime };

    const response = successResponse(
      200,
      "User logged out successfully",
      responseData
    );
    res.status(200).json(response);
  } catch (error) {
    console.error(error);
    const response = badResponse(401, "Failed to logout user");
    res.status(401).json(response);
  }
};

// Forget Password
const forgetPassword = async (req, res) => {
  const { email } = req.body;

  try {
    // Mengirim email reset password
    await firebase.auth().sendPasswordResetEmail(email);

    const response = successResponse(200, "Reset password email has been sent");
    return res.status(200).json(response);
  } catch (error) {
    console.error("Error sending password reset email:", error);
    const response = badResponse(500, "Failed to send password reset email");
    return res.status(500).json(response);
  }
};

// Create Bookmark
const createBookmark = async (req, res) => {
  try {
    const { recipeId } = req.body;

    if (recipeId.length === 0) {
      const response = badResponse(400, "Recipe Id is required");
      return res.status(400).json(response);
    }

    // Ambil data dari API Public
    const recipe = await axios.get(`https://www.themealdb.com/api/json/v1/1/lookup.php?i=${recipeId}`);
    const recipeData = recipe.data.meals ? recipe.data.meals[0] : null;

    if (!recipeData) {
      const response = badResponse(404, "Recipe not found");
      return res.status(404).json(response);
    }

    // Membuat custom bookmark Id (gabungan user Id dan recipe Id)
    const bookmarkId = `${req.uid}_${recipeData.idMeal}`;
  
    const bookmarkData = {
      user_id: req.uid,
      bookmark_id: bookmarkId,
      time: dateTimeNow(),
      recipe: {
        ...recipeData
      }
    }

    await db.collection('bookmarks').doc(bookmarkId).set(bookmarkData);
    
    const response = successResponse(
      201,
      "Recipe success added to bookmark",
      bookmarkData,
    );
    res.status(201).json(response);
  } catch (error) {
    const statusCode = error.statusCode || 500;
    const response = badResponse(
      statusCode,
      "Error while adding recipe to bookmark",
      error.message
    );
    res.status(statusCode).json(response);
  }
};

// Delete Bookmark
const deleteBookmark = async (req, res) => {
  try {
    const bookmarkId = req.params.id;

    if (!bookmarkId) {
      const response = badResponse(400, "Bookmark Id is required");
      return res.status(400).json(response);
    }

    const bookmarkRef = db.collection('bookmarks').doc(bookmarkId);
    const bookmarkDoc = await bookmarkRef.get();

    if (!bookmarkDoc.exists) {
      const response = badResponse(404, "Bookmark not found");
      return res.status(404).json(response);
    }

    await bookmarkRef.delete(); 
    const response = successResponse(
      200,
      "Delete recipe from bookmark successfully",
    );
    res.status(200).json(response);
  } catch (error) {
    const response = badResponse(500, "Error while deleting recipe from bookmark");
    res.status(500).json(response);
  }
};

// Get Bookmark for a user
const getBookmark = async (req, res) => {
  try {
    const bookmarksSnapshot = await db.collection('bookmarks').where('user_id', '==', req.uid).get();

    if (bookmarksSnapshot.empty) {
      const response = badResponse(404, "No bookmarks found");
      return res.status(404).json(response);
    }

    const bookmarks = bookmarksSnapshot.docs.map(doc => doc.data());

    const response = successResponse(
      200,
      "Bookmarks retrieved successfully",
      bookmarks
    );
    res.status(200).json(response);
  } catch (error) {
    console.error('Error getting bookmarks:', error.message);
    const response = badResponse(500, "Error while getting bookmark");
    res.status(500).json(response);
  }
};

export {
  login,
  register,
  logout,
  forgetPassword,
  createBookmark,
  deleteBookmark,
  getBookmark
};