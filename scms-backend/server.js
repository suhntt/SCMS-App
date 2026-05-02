const express = require("express");
const cors = require("cors");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");
const bcrypt = require("bcrypt");
const axios = require("axios");
const cloudinary = require('cloudinary').v2;
const streamifier = require('streamifier');
require('dotenv').config(); // Load .env file
const AIClassifier = require("./classifier");

const SALT_ROUNDS = 10;

// Configure Cloudinary
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || 'YOUR_CLOUD_NAME_HERE',
  api_key: process.env.CLOUDINARY_API_KEY || 'YOUR_API_KEY_HERE',
  api_secret: process.env.CLOUDINARY_API_SECRET || 'YOUR_API_SECRET_HERE'
});

// ===============================
// 📧 NODEMAILER SETUP
// ===============================
// Set SMTP_USER and SMTP_PASS environment variables for real email sending.
// Falls back gracefully to console-log-only mode if not configured.
const mailTransporter = (process.env.SMTP_USER && process.env.SMTP_PASS)
  ? nodemailer.createTransport({
    service: "gmail",
    auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS }
  })
  : null;

async function sendResetEmail(to, resetUrl) {
  const subject = "Reset Your SCMS Password";
  const html = `
    <div style="font-family:Inter,sans-serif;max-width:480px;margin:auto;padding:32px;background:#0F1B2D;color:#E2E8F0;border-radius:12px">
      <h2 style="color:#3B82F6">🔐 Password Reset</h2>
      <p>You requested a password reset for your SCMS account.</p>
      <p>Click the button below to reset your password. This link expires in <strong>1 hour</strong>.</p>
      <a href="${resetUrl}" style="display:inline-block;margin:20px 0;padding:12px 28px;background:#3B82F6;color:#fff;border-radius:8px;text-decoration:none;font-weight:600">Reset Password</a>
      <p style="color:#64748B;font-size:12px">If you did not request this, please ignore this email.</p>
    </div>
  `;

  if (mailTransporter) {
    await mailTransporter.sendMail({ from: `"SCMS" <${process.env.SMTP_USER}>`, to, subject, html });
    console.log(`📧 Password reset email sent to ${to}`);
  } else {
    // Dev fallback — log the link so you can test without SMTP
    console.log(`\n=================================\n📧 MAIL (DEV MODE — no SMTP configured)\n=================================\nTO: ${to}\nSUBJECT: ${subject}\nRESET LINK: ${resetUrl}\n=================================\n(Set SMTP_USER and SMTP_PASS env vars to enable real email sending)\n=================================\n`);
  }
}

const app = express();

// ===============================
// 🔥 FIREBASE INIT
// ===============================
let db = null;
let bucket = null;
try {
  const serviceAccount = require("./serviceAccountKey.json");
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  db = admin.firestore();
  console.log("✅ Firebase Admin SDK Initialised & Connected to LIVE Firestore");
} catch (e) {
  console.log("⚠️ No serviceAccountKey.json found. Enabling LOCAL FIREBASE EMULATOR.");
  console.log("   (Start it in another terminal: npx firebase-tools emulators:start --project demo-scms-local)");

  // Magic: point Firebase SDK to local emulator
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";

  admin.initializeApp({ projectId: "demo-scms-local" });
  db = admin.firestore();
  console.log("✅ Connected to Firebase Local Emulator on 127.0.0.1:8080.");
}

app.use(cors());
app.use(express.json());
app.use("/uploads", express.static(path.join(__dirname, "uploads")));
app.use(express.static(path.join(__dirname, "public")));

// ===============================
// 📁 Multer Setup (Memory Storage → Firebase Storage)
// ===============================
// We use memoryStorage so the file buffer is available for Firebase Storage upload.
// Photos are no longer saved to local disk.
const upload = multer({ storage: multer.memoryStorage() });

// -------------------------------------------------------
// 🪣 Helper: Upload buffer to Cloudinary, return public URL
// -------------------------------------------------------
function uploadToCloudinary(fileBuffer) {
  return new Promise((resolve, reject) => {
    let stream = cloudinary.uploader.upload_stream(
      { folder: "scms_complaints" },
      (error, result) => {
        if (result) {
          console.log(`✅ Photo uploaded to Cloudinary: ${result.secure_url}`);
          resolve(result.secure_url);
        } else {
          reject(error);
        }
      }
    );
    streamifier.createReadStream(fileBuffer).pipe(stream);
  });
}

// -------------------------------------------------------
// 🧠 ADVANCED: AI Semantic Similarity (HuggingFace API)
// -------------------------------------------------------
async function getHFSimilarity(s1, s2) {
  const HF_API_KEY = process.env.HF_API_KEY;
  if (!HF_API_KEY) {
    // Fallback to Jaccard if no API key provided
    console.warn("⚠️ HF_API_KEY missing. Using fallback similarity.");
    const words1 = new Set(s1.toLowerCase().match(/\w+/g));
    const words2 = new Set(s2.toLowerCase().match(/\w+/g));
    const intersection = new Set([...words1].filter(x => words2.has(x)));
    const union = new Set([...words1, ...words2]);
    return intersection.size / (union.size || 1);
  }

  try {
    const response = await axios.post(
      "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2",
      { inputs: { source_sentence: s1, sentences: [s2] } },
      { headers: { Authorization: `Bearer ${HF_API_KEY}` } }
    );
    return response.data[0] || 0;
  } catch (err) {
    console.error("HF Similarity Error:", err.message);
    return 0;
  }
}

// -------------------------------------------------------
// 🗺️ Helper: Haversine Formula (Precise Distance)
// -------------------------------------------------------
function getHaversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c; // Distance in km
}

// -------------------------------------------------------
// 🗺️ Real Reverse Geocoding: Assam District Detection
// Uses Nominatim (OpenStreetMap) — free, no API key needed
// -------------------------------------------------------
async function getDistrict(lat, lon) {
  if (!lat || !lon) return "Unknown District";
  try {
    const response = await axios.get(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&addressdetails=1`,
      {
        headers: { 'User-Agent': 'SCMS-Assam/1.0 (scms@assam.gov.in)' },
        timeout: 5000
      }
    );
    const addr = response.data.address || {};
    // For India: state_district is the true administrative district (e.g. Karimganj)
    // county sometimes returns the Tehsil/Sub-division (e.g. Badarpur)
    let district = addr.state_district || addr.county || addr.city_district ||
                     addr.city || addr.town || addr.village || "Unknown District";
                     
    // Clean up "District" suffix if present
    if (district.toLowerCase().endsWith(" district")) {
        district = district.substring(0, district.length - 9).trim();
    }
    
    console.log(`📍 District detected: ${district} for (${lat}, ${lon})`);
    return district;
  } catch (err) {
    console.warn(`⚠️ District lookup failed for (${lat}, ${lon}):`, err.message);
    return "Unknown District";
  }
}

// -------------------------------------------------------
// 📡 FCM Topic helper: normalize district name to valid topic
// "Kamrup Metropolitan" → "district_kamrup_metropolitan"
// -------------------------------------------------------
function toDistrictTopic(district) {
  if (!district) return "district_unknown";
  return `district_${district.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_').replace(/^_|_$/g, '')}`;
}

// Keep legacy getMunicipality for backward compat in duplicate-check
function getMunicipality(lat, lon) {
  return `(${parseFloat(lat).toFixed(2)}, ${parseFloat(lon).toFixed(2)}) Area`;
}

// ===============================

// 🔢 Auto-sequencer Helper for MySQL-like Integer IDs
// ===============================
async function getNextId(collectionName) {
  if (!db) return Date.now(); // Fallback if no Firebase connected
  const counterRef = db.collection('counters').doc(collectionName);

  try {
    const res = await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(counterRef);
      const newId = doc.exists ? doc.data().seq + 1 : 1;
      transaction.set(counterRef, { seq: newId });
      return newId;
    });
    return res;
  } catch (err) {
    console.error(`ID Generation Error for ${collectionName}:`, err);
    return Date.now(); // emergency fallback
  }
}

// ================================================
// 🔐 AUTH (Signup/Login into Firestore users collection)
// ================================================

app.post("/signup", async (req, res) => {
  const { name, phone, email, password } = req.body;
  if (!name || !phone || !password)
    return res.status(400).json({ success: false, message: "Name, phone, and password required" });

  if (!/^\d{10}$/.test(phone)) {
    return res.status(400).json({ success: false, message: "Phone number must be exactly 10 digits" });
  }

  try {
    // Check dupe phone
    const snapshot = await db.collection("users").where("phone", "==", phone).get();
    if (!snapshot.empty) {
      return res.status(409).json({ success: false, message: "Phone already registered" });
    }

    const newId = await getNextId("users");

    // 🔐 Hash password with bcrypt before storing
    const hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);

    await db.collection("users").doc(newId.toString()).set({
      id: newId,
      name,
      phone,
      email: email ? email.toLowerCase() : null,
      password: hashedPassword, // ✅ Stored as bcrypt hash
      points: 0,
      badgeLevel: "Citizen",
      fcm_token: null,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    res.json({ success: true, userId: newId });
  } catch (err) {
    console.error("❌ Signup error:", err);
    res.status(500).json({ success: false });
  }
});

// ✅ UPDATE FCM TOKEN
app.put("/user/token", async (req, res) => {
  const { user_id, fcm_token } = req.body;
  if (!user_id || !fcm_token) return res.status(400).json({ success: false });

  try {
    await db.collection("users").doc(user_id.toString()).update({ fcm_token });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

app.post("/login", async (req, res) => {
  const { phone, password } = req.body;
  if (!phone || !password)
    return res.status(400).json({ success: false, message: "Phone and password required" });

  if (!/^\d{10}$/.test(phone)) {
    return res.status(400).json({ success: false, message: "Phone number must be exactly 10 digits" });
  }

  try {
    // Fetch user by phone, then compare password with bcrypt
    const snapshot = await db.collection("users")
      .where("phone", "==", phone)
      .limit(1)
      .get();

    if (snapshot.empty) {
      return res.status(401).json({ success: false, message: "Invalid credentials" });
    }

    const userData = snapshot.docs[0].data();

    // 🔐 bcrypt.compare handles both hashed and plain-text legacy passwords gracefully
    const passwordMatch = await bcrypt.compare(password, userData.password).catch(() => false);
    // Legacy fallback: also allow plain-text match (for existing users before hashing was added)
    const legacyMatch = !passwordMatch && (userData.password === password);

    if (!passwordMatch && !legacyMatch) {
      return res.status(401).json({ success: false, message: "Invalid credentials" });
    }

    // If legacy user logged in with plain text, transparently upgrade their hash
    if (legacyMatch) {
      const upgraded = await bcrypt.hash(password, SALT_ROUNDS);
      await db.collection("users").doc(snapshot.docs[0].id).update({ password: upgraded });
      console.log(`🔄 Upgraded plain-text password to bcrypt hash for user ${userData.id}`);
    }

    res.json({
      success: true,
      user: {
        id: userData.id,
        name: userData.name,
        phone: userData.phone,
        email: userData.email || null,
        profile_picture: userData.profile_picture || null,
        points: userData.points || 0,
        badgeLevel: userData.badgeLevel || "Citizen"
      }
    });
  } catch (err) {
    console.error("❌ Login error:", err);
    res.status(500).json({ success: false });
  }
});

app.post("/forgot-password", async (req, res) => {
  const { email } = req.body;

  if (!email) return res.status(400).json({ success: false, message: "Email required" });
  if (!email.toLowerCase().endsWith("@gmail.com")) return res.status(400).json({ success: false, message: "Only Gmail addresses are supported for reset" });

  try {
    let query = db.collection("users").where("email", "==", email.toLowerCase()).limit(1);
    const snapshot = await query.get();

    if (snapshot.empty) return res.status(404).json({ success: false, message: "Email not found. If you don't have an email set, please contact support." });

    const userDoc = snapshot.docs[0];

    // Generate a secure mock reset token
    const token = crypto.randomBytes(20).toString("hex");

    // Save to Firestore with timestamp logic
    await db.collection("users").doc(userDoc.id).update({
      resetPasswordToken: token,
      resetPasswordExpires: Date.now() + 3600000 // 1 hour
    });

    // Generate the reset link
    const rootUrl = `http://localhost:3000/reset-password.html?token=${token}`;

    // 📧 Send real email (or log if SMTP not configured)
    await sendResetEmail(email, rootUrl);

    res.json({ success: true, message: "Password reset instructions sent to your email!", resetUrl: rootUrl });
  } catch (err) {
    console.error("❌ Forgot password error:", err);
    res.status(500).json({ success: false });
  }
});

// Mocking the Password Update action from the email Token Link form representation
app.post("/reset-password", async (req, res) => {
  const { token, newPassword } = req.body;
  if (!token || !newPassword) return res.status(400).json({ success: false, message: "Token and password required." });

  try {
    const snapshot = await db.collection("users")
      .where("resetPasswordToken", "==", token)
      .where("resetPasswordExpires", ">", Date.now())
      .limit(1)
      .get();

    if (snapshot.empty) return res.status(400).json({ success: false, message: "Password reset token is invalid or has expired." });

    const userDoc = snapshot.docs[0];

    // 🔐 Hash the new password before saving
    const hashedNew = await bcrypt.hash(newPassword, SALT_ROUNDS);
    await db.collection("users").doc(userDoc.id).update({
      password: hashedNew,
      resetPasswordToken: null,
      resetPasswordExpires: null
    });

    res.json({ success: true, message: "Password successfully updated!" });
  } catch (error) {
    res.status(500).json({ success: false });
  }
});

// ================================================
// 🛑 CHECK DUPLICATE COMPLAINT
// ================================================
app.post("/complaints/check-duplicate", async (req, res) => {
  const { category, latitude, longitude, description } = req.body;
  if (!category || !latitude || !longitude) {
    return res.status(400).json({ success: false, message: "Missing required fields" });
  }

  try {
    const lat1 = parseFloat(latitude);
    const lon1 = parseFloat(longitude);
    const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const municipality = getMunicipality(lat1, lon1);

    const snapshot = await db.collection("complaints")
      .where("category", "==", category)
      .get();

    let duplicate = null;

    for (const doc of snapshot.docs) {
      const data = doc.data();
      
      // Temporal constraint
      let isRecent = true;
      if (data.created_at) {
        const createdAt = data.created_at.toDate ? data.created_at.toDate() : new Date(data.created_at);
        isRecent = createdAt >= oneDayAgo;
      }

      if (data.latitude && data.longitude && isRecent) {
        const dist = getHaversineDistance(lat1, lon1, parseFloat(data.latitude), parseFloat(data.longitude));
        const currentMunicipality = getMunicipality(data.latitude, data.longitude);

        // Advanced Logic: Same Municipality + Same Category + Distance < 100m (0.1km)
        if (dist <= 0.1 && municipality === currentMunicipality && data.status !== 'Resolved') {
          // AI Semantic Similarity Check
          const similarity = await getHFSimilarity(description || "", data.description || "");
          if (similarity > 0.75) {
            duplicate = { id: data.id, description: data.description, distance: dist, similarity };
            break;
          }
        }
      }
    }

    if (duplicate) {
      res.json({ success: true, isDuplicate: true, duplicate });
    } else {
      res.json({ success: true, isDuplicate: false });
    }
  } catch (err) {
    console.error("❌ Check duplicate error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 📝 SUBMIT COMPLAINT (+10 pts to reporter)
// ================================================
app.post("/complaint", upload.array("photos", 5), async (req, res) => {
  const { category, address, latitude, longitude, description, user_id, district: clientDistrict } = req.body;

  const place = (address && address.trim() !== "") ? address.trim() : "Address not available";
  const uid = user_id ? parseInt(user_id) : null;
  const lat = latitude !== undefined ? latitude : null;
  const lon = longitude !== undefined ? longitude : null;

  try {
    // 🪣 Upload photos to Cloudinary (if provided)
    // ⚠️ If Cloudinary fails, we still save the complaint without a photo
    let photoUrls = [];
    if (req.files && req.files.length > 0) {
      try {
        for (const file of req.files) {
          const url = await uploadToCloudinary(file.buffer);
          photoUrls.push(url);
        }
      } catch (uploadErr) {
        console.warn("⚠️ Cloudinary upload failed (complaint will be saved without photo):", uploadErr.message || uploadErr);
      }
    }

    let photoUrl = photoUrls.length > 0 ? photoUrls[0] : null;

    // 🧠 AI PROCESSOR: Predict Severity + Category if applicable
    const prediction = await AIClassifier.classifyComplaint(description || category || "Unknown Hazard");
    let finalCategory = category || prediction.suggestedCategory;
    let finalSeverity = prediction.severity;

    const newId = await getNextId("complaints");

    // 🏢 ADMIN ROUTING SYSTEM: Detect Assam district via Nominatim reverse geocoding
    // Use district sent from app (already detected via Geocoder) or detect server-side
    const detectedDistrict = clientDistrict || await getDistrict(lat, lon);
    const districtTopic = toDistrictTopic(detectedDistrict);

    let assignedDepartment = "General";
    if (finalCategory === "Road Damage") assignedDepartment = "Public Works";
    else if (finalCategory === "Water Leakage" || finalCategory === "Water Supply") assignedDepartment = "Water Supply";
    else if (finalCategory === "Streetlight Issue" || finalCategory === "Electricity") assignedDepartment = "Electricity Board";
    else if (finalCategory === "Illegal Dumping" || finalCategory === "Waste Management") assignedDepartment = "Waste Management";
    else if (finalCategory === "Public Safety") assignedDepartment = "Police / Civil Defence";
    
    assignedDepartment += ` [${detectedDistrict}]`;

    await db.collection("complaints").doc(newId.toString()).set({
      id: newId,
      user_id: uid,
      title: req.body.title || finalCategory,
      category: finalCategory,
      description,
      place,
      latitude: lat,
      longitude: lon,
      photoUrl: photoUrl,
      photo_urls: photoUrls,
      status: "Pending",
      district: detectedDistrict,         // ✅ Assam district field
      districtTopic: districtTopic,       // ✅ FCM topic for this district
      status_history: [{
        status: "Pending",
        note: "Complaint submitted by citizen",
        timestamp: admin.firestore.Timestamp.now()
      }],
      upvotes: 0,
      impactScore: finalSeverity === 'High' ? 100 : finalSeverity === 'Medium' ? 50 : 10,
      severity: finalSeverity,
      ai_confidence: prediction.confidence,
      department: assignedDepartment,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // 🚀 BROADCAST TO DISTRICT USERS via FCM Topic
    // Only users in the same Assam district receive this notification
    if (lat && lon && admin.apps.length > 0) {
      admin.messaging().send({
        topic: districtTopic,
        notification: {
          title: `🚨 New Issue in ${detectedDistrict}: ${finalCategory}`,
          body: `Reported near: ${place}`
        },
        data: {
          type: "incident",
          district: detectedDistrict,
          complaintId: newId.toString()
        }
      }).catch(e => console.error("District broadcast failed:", e));
      console.log(`📡 Broadcast sent to FCM topic: ${districtTopic}`);
    }

    // 👤 IDENTIFY REPORTER
    let reporterName = "Anonymous Citizen";
    if (user_id) {
      try {
        const userRef = db.collection("users").doc(user_id.toString());
        const udoc = await userRef.get();
        if (udoc.exists) {
          const userData = udoc.data();
          reporterName = userData.name || "Anonymous Citizen";
          
          // Award 10 points
          await userRef.update({
            points: admin.firestore.FieldValue.increment(10)
          });
          console.log(`✅ Awarded 10 points to ${reporterName}`);
        }
      } catch (e) {
        console.error("User profile/points update error:", e);
      }
    }

    // 📢 BROADCAST ALERT TO DISTRICT USERS ONLY 📢
    try {
      const newAlertId = await getNextId("alerts");
      await db.collection("alerts").doc(newAlertId.toString()).set({
        title: finalCategory,
        message: description || "No description provided.",
        reporterName: reporterName,
        complaintId: newId.toString(),
        type: finalSeverity === 'High' ? "danger" : (finalSeverity === 'Medium' ? "warning" : "info"),
        area: place,
        latitude: lat,
        longitude: lon,
        district: detectedDistrict,        // ✅ District-tagged alert
        districtTopic: districtTopic,
        photoUrl: photoUrl,
        created_at: admin.firestore.FieldValue.serverTimestamp()
      });

      // Send FCM only to the same district's users
      if (admin.apps.length > 0) {
        admin.messaging().send({
          topic: districtTopic,
          notification: {
            title: `🚨 ${finalSeverity === 'High' ? 'High Severity' : 'New Issue'} in ${detectedDistrict}`,
            body: `${reporterName} reported ${finalCategory} near ${place}.`
          },
          data: {
            "complaintId": newId.toString(),
            "district": detectedDistrict,
            "screen": "complaints"
          }
        }).catch(e => console.log("FCM district broadcast failed:", e));
      }
    } catch (alertErr) {
      console.error("Alert broadcast error:", alertErr);
    }

    res.json({ success: true, complaintId: newId });
  } catch (err) {
    console.error("❌ Insert error:", err);
    res.status(500).json({ success: false });
  }
});

// ✅ REPLICATED ENDPOINT AS REQUESTED
app.post("/submit-complaint", upload.array("photos", 5), async (req, res) => {
  // Logic is identical to /complaint
  req.url = "/complaint";
  app._router.handle(req, res);
});

// ================================================
// 📋 GET ALL COMPLAINTS (With Scalability Filtering)
// ================================================
// ================================================
// 🗺️ GET DISTRICT FROM COORDINATES (Nominatim)
// Called by Android app for accurate Assam district detection
// ================================================
app.get("/district", async (req, res) => {
  const { lat, lon } = req.query;
  if (!lat || !lon) return res.status(400).json({ error: "lat and lon required" });
  const district = await getDistrict(lat, lon);
  res.json({ district });
});

// ================================================
// 🗺️ SAVE USER DISTRICT (called after location detection)
// ================================================
app.put("/user/district", async (req, res) => {
  const { user_id, district } = req.body;
  if (!user_id || !district) return res.status(400).json({ success: false, message: "user_id and district required" });

  try {
    await db.collection("users").doc(user_id.toString()).set({
      district,
      districtTopic: toDistrictTopic(district),
      district_updated_at: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    console.log(`✅ District saved for user ${user_id}: ${district}`);
    res.json({ success: true, district, districtTopic: toDistrictTopic(district) });
  } catch (err) {
    console.error("❌ District update error:", err);
    res.status(500).json({ success: false });
  }
});

app.get("/complaints", async (req, res) => {
  const { lat, lon, radius, municipality, district } = req.query;

  try {
    // ✅ Fetch everything for in-memory processing (Small scale)
    // In a massive scale, we'd use Firestore Geo-queries (GeoHashes)
    const snapshot = await db.collection("complaints").get();

    // Fetch user names (Like a SQL JOIN)
    const allUsersSnap = await db.collection("users").get();
    const userMap = {};
    allUsersSnap.forEach(doc => {
      userMap[doc.data().id] = doc.data();
    });

    let data = snapshot.docs.map(doc => {
      const row = doc.data();
      const user = userMap[row.user_id] || {};

      const createdStr = row.created_at ? new Date(row.created_at.toMillis()).toISOString() : null;

      // AI Impact Score
      let impact = (row.upvotes || 0) * 5;
      if (row.severity === 'High') impact += 50;
      else if (row.severity === 'Medium') impact += 30;
      else if (row.severity === 'Low') impact += 10;
      else impact += 5;

      return {
        id: row.id,
        category: row.category,
        description: row.description,
        address: row.place,
        district: row.district || null,   // ✅ Include district in response
        latitude: row.latitude,
        longitude: row.longitude,
        created_at: createdStr,
        status: row.status,
        upvotes: row.upvotes || 0,
        severity: row.severity,
        impact_score: impact,
        ai_confidence: row.ai_confidence,
        department: row.department,
        user_id: row.user_id,
        reporter_name: user.name || "Anonymous",
        photo_url: row.photoUrl || null,
        photo_urls: row.photo_urls || (row.photoUrl ? [row.photoUrl] : [])
      };
    });

    // 🗺️ DISTRICT FILTER: Show complaints from user's Assam district OR legacy/system complaints
    if (district) {
      data = data.filter(c => 
        !c.district ||                                             // Legacy test data / system-wide
        c.district.toLowerCase() === district.toLowerCase()        // District-specific
      );
    }

    // 📍 SCALABILITY: Location-based Filtering (fallback if no district)
    if (!district && lat && lon && radius) {
      const l1 = parseFloat(lat);
      const r1 = parseFloat(lon);
      const rad = parseFloat(radius);
      data = data.filter(c => {
        if (!c.latitude || !c.longitude) return false;
        const dist = getDistanceFromLatLonInKm(l1, r1, parseFloat(c.latitude), parseFloat(c.longitude));
        return dist <= rad;
      });
    }

    // 🏢 Legacy: Administrative Region Filtering
    if (municipality) {
      data = data.filter(c => c.department && c.department.toLowerCase().includes(municipality.toLowerCase()));
    }

    // Sort by AI Impact Score, then newest
    data.sort((a, b) => {
      if (b.impact_score === a.impact_score) {
        return new Date(b.created_at) - new Date(a.created_at);
      }
      return b.impact_score - a.impact_score;
    });

    res.json(data);
  } catch (err) {
    console.error("❌ Fetch error:", err);
    res.status(500).json([]);
  }
});

// ================================================
// 👍 UPVOTE (+2 pts to original reporter)
// ================================================
app.post("/upvote/:id", async (req, res) => {
  const compId = req.params.id;
  const { user_id } = req.body;

  if (!user_id) return res.status(400).json({ success: false, message: "user_id required" });

  try {
    const compRef = db.collection("complaints").doc(compId.toString());
    const doc = await compRef.get();

    if (!doc.exists) return res.status(404).json({ success: false });

    const row = doc.data();
    const upvoted_by = row.upvoted_by || [];

    // Toggle logic: If user already upvoted, remove it. Otherwise, add it.
    if (upvoted_by.includes(user_id)) {
      await compRef.update({
        upvotes: admin.firestore.FieldValue.increment(-1),
        upvoted_by: admin.firestore.FieldValue.arrayRemove(user_id)
      });
      // remove 2 pts 
      if (row.user_id) {
        try {
          await db.collection("users").doc(row.user_id.toString()).update({
            points: admin.firestore.FieldValue.increment(-2)
          });
        } catch (e) { }
      }
      return res.json({ success: true, action: "removed" });
    }

    // New upvote
    await compRef.update({
      upvotes: admin.firestore.FieldValue.increment(1),
      upvoted_by: admin.firestore.FieldValue.arrayUnion(user_id)
    });

    // award 2 pts to reporter
    if (row.user_id) {
      try {
        await db.collection("users").doc(row.user_id.toString()).update({
          points: admin.firestore.FieldValue.increment(2)
        });
      } catch (e) { }
    }

    res.json({ success: true, action: "added" });
  } catch (err) {
    console.error("❌ Upvote error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 🏢 ASSIGN DEPARTMENT
// ================================================
app.put("/complaint/department/:id", async (req, res) => {
  try {
    const compRef = db.collection("complaints").doc(req.params.id);
    const updates = {
      department: req.body.department,
      status: "Assigned",
      status_history: admin.firestore.FieldValue.arrayUnion({
        status: "Assigned",
        note: `Assigned to ${req.body.department} department`,
        timestamp: admin.firestore.Timestamp.now()
      })
    };
    await compRef.update(updates);
    res.json({ success: true });
  } catch (err) {
    console.error("❌ Assign dept error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 📋 GET STATUS HISTORY for a single complaint
// ================================================
app.get("/complaint/:id/history", async (req, res) => {
  try {
    const doc = await db.collection("complaints").doc(req.params.id).get();
    if (!doc.exists) return res.status(404).json([]);
    const data = doc.data();
    const history = (data.status_history || []).map(h => ({
      status: h.status,
      timestamp: h.timestamp ? new Date(h.timestamp.toMillis()).toISOString() : null,
      note: h.note || null
    }));
    res.json(history);
  } catch (err) {
    console.error("❌ Status history error:", err);
    res.status(500).json([]);
  }
});

// ================================================
// 🔄 UPDATE COMPLAINT STATUS (with history tracking)
// ================================================
app.put("/complaint/status/:id", async (req, res) => {
  const { status, note } = req.body;
  const validStatuses = ["Pending", "Verified", "Assigned", "In Progress", "Resolved", "Escalated"];
  if (!status || !validStatuses.includes(status)) {
    return res.status(400).json({ success: false, message: "Invalid status value" });
  }

  const compId = req.params.id;
  try {
    const compRef = db.collection("complaints").doc(compId);
    const doc = await compRef.get();
    if (!doc.exists) return res.status(404).json({ success: false });
    const row = doc.data();

    // Build the new history entry
    const historyEntry = {
      status,
      note: note || null,
      timestamp: admin.firestore.Timestamp.now()
    };

    const updates = {
      status,
      status_history: admin.firestore.FieldValue.arrayUnion(historyEntry)
    };

    // If resolving, also award 20 points (same as /complaint/resolve/:id)
    if (status === "Resolved" && row.user_id) {
      await db.collection("users").doc(row.user_id.toString())
        .update({ points: admin.firestore.FieldValue.increment(20) })
        .catch(() => { });
    }

    await compRef.update(updates);

    // 📲 Push notification to the reporter if FCM token exists
    if (row.user_id && admin.apps.length > 0) {
      const userDoc = await db.collection("users").doc(row.user_id.toString()).get();
      const token = userDoc.data()?.fcm_token;
      if (token) {
        const statusMessages = {
          "Verified": "Your complaint has been verified by SCMS staff.",
          "Assigned": `Your complaint has been assigned to the ${row.department || 'relevant'} department.`,
          "In Progress": "Work on your complaint has started! Our team is on the ground.",
          "Resolved": "Great news! Your complaint has been resolved. You earned +20 points! 🎉"
        };
        const body = statusMessages[status] || `Your complaint status changed to: ${status}`;
        admin.messaging().send({
          token,
          notification: {
            title: `📊 Status Update: ${status}`,
            body
          },
          data: {
            screen: "timeline",
            complaintId: compId
          }
        }).catch(e => console.error("FCM Status Push Error:", e));
      }
    }

    console.log(`✅ Complaint #${compId} status → ${status}`);
    res.json({ success: true });
  } catch (err) {
    console.error("❌ Status update error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// ✅ MARK RESOLVED WITH PROOF PHOTO
// ================================================
app.post("/complaint/resolve-proof/:id", upload.single("photo"), async (req, res) => {
  const compId = req.params.id;
  try {
    const compRef = db.collection("complaints").doc(compId.toString());
    const doc = await compRef.get();
    if (!doc.exists) return res.status(404).json({ success: false });

    let resolvedPhotoUrl = null;
    if (req.file) {
      resolvedPhotoUrl = await uploadToCloudinary(req.file.buffer);
    }

    const updates = {
      status: 'Resolved',
      resolved_photo_url: resolvedPhotoUrl,
      status_history: admin.firestore.FieldValue.arrayUnion({
        status: "Resolved",
        note: "Issue resolved by Admin with proof photo",
        timestamp: admin.firestore.Timestamp.now()
      })
    };

    await compRef.update(updates);

    const row = doc.data();
    if (row.user_id) {
      await db.collection("users").doc(row.user_id.toString()).update({
        points: admin.firestore.FieldValue.increment(20)
      });
    }

    res.json({ success: true, resolved_photo_url: resolvedPhotoUrl });
  } catch (err) {
    console.error("❌ Resolve proof error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// ✅ MARK RESOLVED (+20 pts to reporter + FCM Push)
// ================================================
app.post("/complaint/resolve/:id", async (req, res) => {
  const compId = req.params.id;

  try {
    const compRef = db.collection("complaints").doc(compId.toString());
    const doc = await compRef.get();

    if (!doc.exists) return res.status(404).json({ success: false });

    await compRef.update({ status: 'Resolved' });

    const row = doc.data();
    if (row.user_id) {
      const userRef = db.collection("users").doc(row.user_id.toString());

      // 20 points
      await userRef.update({
        points: admin.firestore.FieldValue.increment(20)
      });

      // Trigger FCM Push notification if token exists
      const userDoc = await userRef.get();
      const token = userDoc.data()?.fcm_token;
      if (token && admin.apps.length > 0) {
        admin.messaging().send({
          token: token,
          notification: {
            title: "✅ Complaint Resolved!",
            body: "Good news! Admin has resolved your issue. You earned +20 points."
          }
        }).catch(err => console.error("FCM Send Error:", err));
      }
    }

    res.json({ success: true });
  } catch (err) {
    console.error("❌ Resolve error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 🏆 LEADERBOARD (top 20 citizens by points)
// ================================================
app.get("/leaderboard", async (req, res) => {
  try {
    // 1. Get Top 20 Users by points directly from 'users'
    const usersSnap = await db.collection("users")
      .orderBy("points", "desc")
      .limit(20)
      .get();

    // Because we lack true JOINs in NoSQL, and we need total/resolved complaints counts,
    // we fetch ALL complaints to aggregate (fast enough for small-medium scales)
    const complSnap = await db.collection("complaints").get();

    // Build user -> metrics map
    const metrics = {};
    complSnap.forEach(doc => {
      const c = doc.data();
      if (c.user_id) {
        if (!metrics[c.user_id]) {
          metrics[c.user_id] = { total: 0, resolved: 0, upvotes: 0 };
        }
        metrics[c.user_id].total += 1;
        if (c.status === 'Resolved') metrics[c.user_id].resolved += 1;
        metrics[c.user_id].upvotes += (c.upvotes || 0);
      }
    });

    const leaderboard = usersSnap.docs.map(doc => {
      const u = doc.data();
      const m = metrics[u.id] || { total: 0, resolved: 0, upvotes: 0 };
      return {
        id: u.id,
        name: u.name,
        points: u.points || 0,
        total_complaints: m.total,
        resolved_complaints: m.resolved,
        total_upvotes: m.upvotes
      };
    });

    res.json(leaderboard);
  } catch (err) {
    console.error("❌ Leaderboard error:", err);
    res.status(500).json([]);
  }
});

// ===============================
// 🚨 ADMIN BROADCAST: Create Alerts (Disaster, News, etc.)
// ===============================
app.post("/admin/alerts", async (req, res) => {
  try {
    const { title, message, type, area, district } = req.body;
    const newId = (Date.now() % 1000000).toString();
    const distTopic = district ? toDistrictTopic(district) : "all_users";

    await db.collection("alerts").doc(newId).set({
      title: title || "Emergency Alert",
      message: message || "Stay safe!",
      type: type || "danger",
      area: area || (district ? `${district} District` : "All Districts"),
      district: district || null,          // null = broadcast to all
      districtTopic: distTopic,
      reporterName: "SCMS System Admin",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // Send FCM to specific district OR all users
    if (admin.apps.length > 0) {
      admin.messaging().send({
        topic: distTopic,
        notification: {
          title: `📢 Admin Alert${district ? ` — ${district}` : ''}: ${title}`,
          body: message
        },
        data: { type: type || "danger", district: district || "all" }
      }).catch(e => console.error("Admin FCM alert error:", e));
    }

    console.log(`📡 Admin Broadcast to [${distTopic}]: ${title}`);
    res.json({ success: true, id: newId, districtTopic: distTopic });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ===============================
// 🏆 POINTS MASTER: Adjust User Points
// ===============================
app.post("/admin/user-points", async (req, res) => {
  try {
    const { userId, points } = req.body;
    const docRef = db.collection("users").doc(userId);
    const doc = await docRef.get();
    if (!doc.exists) return res.status(404).json({ error: "User not found" });

    const newPoints = (doc.data().points || 0) + parseInt(points);
    let badge = "Citizen";
    if (newPoints > 500) badge = "Guardian";
    if (newPoints > 1000) badge = "Hero";

    await docRef.update({ points: newPoints, badgeLevel: badge });
    res.json({ success: true, newPoints, badge });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// GAMIFICATION: GET USER POINTS
// ===============================
app.get("/user/:id/points", async (req, res) => {
  try {
    const doc = await db.collection("users").doc(req.params.id).get();
    if (!doc.exists) return res.status(404).json({ error: "User not found" });
    res.json({
      points: doc.data().points || 0,
      badgeLevel: doc.data().badgeLevel || "Citizen",
      name: doc.data().name,
      email: doc.data().email
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// PROFILE UPDATE: PHOTO & INFO
// ===============================
app.post("/user/:id/profile", upload.single("photo"), async (req, res) => {
  try {
    let updates = {};
    if (req.file) {
      try {
        const photoUrl = await uploadToCloudinary(req.file.buffer);
        updates.profile_picture = photoUrl;
      } catch (uploadErr) {
        console.warn("⚠️ Cloudinary upload failed:", uploadErr);
        return res.status(500).json({ success: false, message: "Photo upload failed" });
      }
    }

    if (Object.keys(updates).length > 0) {
      await db.collection("users").doc(req.params.id).update(updates);
    }
    res.json({ success: true, profile_picture: updates.profile_picture });
  } catch (err) {
    console.error("❌ Profile update error:", err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// ===============================
// 🧪 MOCK DISASTER TESTER (Manually trigger disaster alerts)
// ===============================
app.get("/test-disaster", async (req, res) => {
  try {
    const simType = req.query.type || "storm";

    let title = "";
    let message = "";

    if (simType === "earthquake") {
      title = "🫨 EARTHQUAKE ALERT (Mag 6.2)";
      message = "IMD & USGS REPORT: A magnitude 6.2 earthquake just occurred near your city. Drop, Cover, and Hold On! Stay away from windows.";
    } else if (simType === "flood") {
      title = "🌧️ Heavy Rain / Flood Alert (IMD Alert)";
      message = "IMD REPORT: Extremely heavy rainfall detected. Risk of localized flooding increased. Avoid driving through flooded roads.";
    } else {
      title = "⛈️ Thunderstorm Warning (IMD Alert)";
      message = "IMD REPORT: Severe thunderstorm and lightning detected. Please stay indoors and avoid using electrical appliances. Be safe!";
    }

    const newId = await getNextId("alerts");
    await db.collection("alerts").doc(newId.toString()).set({
      title,
      message,
      type: "danger",
      area: "System Auto-Detection",
      reporterName: "SCMS National Disasters Bot",
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    if (admin.apps.length > 0) {
      await admin.messaging().send({
        topic: "all_users",
        notification: { title, body: message },
        data: { "screen": "alerts" }
      });
    }

    res.json({ success: true, message: `Mock disaster (${simType}) broadcasted!`, title });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// PUSH NOTIFICATIONS: UPDATE FCM TOKEN
// ===============================
app.post("/user/:id/fcm", async (req, res) => {
  try {
    await db.collection("users").doc(req.params.id).update({
      fcm_token: req.body.fcm_token
    });
    res.json({ message: "Token updated" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// API: ALERTS & ACCIDENTS
// ===============================
app.get("/alerts", async (req, res) => {
  const { district } = req.query;
  try {
    const snap = await db.collection("alerts").orderBy("created_at", "desc").get();
    let alerts = snap.docs.map(doc => {
      const data = doc.data();
      let formattedTime = null;
      if (data.created_at && data.created_at.toDate) {
        formattedTime = data.created_at.toDate().toISOString();
      }
      return { alertId: doc.id, ...data, created_at: formattedTime };
    });

    // 🗺️ Filter alerts by district — system alerts (no district) are shown to everyone
    if (district) {
      alerts = alerts.filter(a =>
        !a.district ||                                              // system-wide alerts shown to all
        a.district.toLowerCase() === district.toLowerCase()        // district-specific alerts
      );
    }

    res.json(alerts);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post("/alerts", async (req, res) => {
  const { title, message, type, area } = req.body;
  if (!title || !message) return res.status(400).json({ success: false, message: "Title and message required" });

  try {
    const newId = await getNextId("alerts");
    await db.collection("alerts").doc(newId.toString()).set({
      title,
      message,
      type: type || "info",
      area: area || "",
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // FCM broadcast to all_users topic
    if (admin.apps.length > 0) {
      admin.messaging().send({
        topic: "all_users",
        notification: {
          title: `🚨 ${type === 'danger' ? 'Emergency Alert' : 'System Alert'}: ${title}`,
          body: message
        }
      }).catch(err => console.error("FCM Broadcast Error:", err));
    }

    res.json({ success: true, alertId: newId });
  } catch (err) {
    console.error("❌ Post alert error:", err);
    res.status(500).json({ success: false });
  }
});

app.post("/accidents", async (req, res) => {
  const { latitude, longitude } = req.body;
  if (latitude === undefined || longitude === undefined) return res.status(400).json({ error: "Missing coords" });
  try {
    const newId = await getNextId("accidents");
    await db.collection("accidents").doc(newId.toString()).set({
      accidentId: newId,
      latitude,
      longitude,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // Broadcast Emergency Alert
    if (admin.apps.length > 0) {
      admin.messaging().send({
        topic: "all_users",
        notification: {
          title: "🚨 Emergency SOS Reported",
          body: "An accident was just reported near your area. Please stay safe and clear the roads for emergency services."
        }
      }).catch(err => console.error("FCM SOS Error:", err));
    }

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ===============================
// ACCIDENT BLACKSPOTS ALGORITHM
// ===============================
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  var R = 6371; // Radius of the earth in km
  var dLat = deg2rad(lat2 - lat1);  // deg2rad below
  var dLon = deg2rad(lon2 - lon1);
  var a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}
function deg2rad(deg) { return deg * (Math.PI / 180) }

app.get("/blackspots", async (req, res) => {
  try {
    const snap = await db.collection("accidents").get();
    const accidents = snap.docs.map(d => d.data());

    let clusters = [];
    let visited = new Set();

    // Spatial clustering: Group accidents within a 1km radius
    for (let i = 0; i < accidents.length; i++) {
      if (visited.has(i)) continue;
      let cluster = [accidents[i]];
      visited.add(i);

      for (let j = i + 1; j < accidents.length; j++) {
        if (visited.has(j)) continue;
        let dist = getDistanceFromLatLonInKm(
          parseFloat(accidents[i].latitude), parseFloat(accidents[i].longitude),
          parseFloat(accidents[j].latitude), parseFloat(accidents[j].longitude)
        );
        if (dist <= 1.0) { // 1 km radius
          cluster.push(accidents[j]);
          visited.add(j);
        }
      }
      if (cluster.length >= 2) {
        // Center point of the blackspot cluster
        let avgLat = cluster.reduce((sum, a) => sum + parseFloat(a.latitude), 0) / cluster.length;
        let avgLon = cluster.reduce((sum, a) => sum + parseFloat(a.longitude), 0) / cluster.length;
        clusters.push({ lat: avgLat, lng: avgLon, incidentCount: cluster.length });
      }
    }

    res.json(clusters);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// SOCIAL MEDIA LISTENING
// ===============================
app.get("/social-listening", async (req, res) => {
  try {
    const query = "pothole OR accident OR drain OR waterlog OR garbage OR road";
    const url = `https://www.reddit.com/r/india/search.json?q=${encodeURIComponent(query)}&restrict_sr=on&sort=new&t=month&limit=15`;

    const response = await axios.get(url, { headers: { 'User-Agent': 'SCMS_Bot/1.0' } });

    const posts = response.data.data.children.map(child => {
      const d = child.data;
      const text = d.title.toLowerCase();
      const isHigh = text.includes('accident') || text.includes('flood') || text.includes('death') || text.includes('emergency');

      return {
        id: d.id,
        platform: 'Reddit',
        author: d.author,
        title: d.title,
        url: `https://reddit.com${d.permalink}`,
        score: d.score,
        comments: d.num_comments,
        severity: isHigh ? 'High' : 'Medium',
        created_utc: d.created_utc * 1000
      };
    });

    res.json(posts);
  } catch (err) {
    console.error("Social Listening fallback activated:", err.message);
    const fallbacks = [
      { id: "mock1", platform: "Reddit", author: "citizen_xyz", title: "Massive pothole on Ring Road causing accidents everyday", url: "#", score: 145, comments: 34, severity: "High", created_utc: Date.now() },
      { id: "mock2", platform: "Twitter/X", author: "mumbai_updates", title: "Waterlogging at Andheri subway again. BMC please fix!!", url: "#", score: 890, comments: 120, severity: "High", created_utc: Date.now() - 3600000 },
      { id: "mock3", platform: "Mastodon", author: "green_city", title: "Garbage unattended in Sector 4 for 3 straight days.", url: "#", score: 12, comments: 2, severity: "Medium", created_utc: Date.now() - 7200000 }
    ];
    res.json(fallbacks);
  }
});

// ===============================
// 🌪️ AUTOMATED NATURAL DISASTER CHECKER
// ===============================
/**
 * Automatically checks for severe weather conditions 
 * (Thunderstorms, Heavy Rain, Floods) in the user's city/area.
 * If detected, it broadcasts an Alert to all users.
 */
const CHECK_INTERVAL = 30 * 60 * 1000; // Check every 30 minutes
const DEFAULT_LAT = 24.8333; // Default area: Silchar/Assam (Adjust to your city)
const DEFAULT_LON = 92.7789;

async function checkNaturalDisasters() {
  console.log("🔍 Checking for natural disasters/severe weather...");
  try {
    // ============================================
    // 1. WEATHER CHECK (Rain, Wind, Thunderstorms)
    // ============================================
    const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${DEFAULT_LAT}&longitude=${DEFAULT_LON}&current=temperature_2m,rain,wind_speed_10m,weather_code`;
    const weatherRes = await axios.get(weatherUrl);
    const { weather_code, rain, wind_speed_10m } = weatherRes.data.current;

    let title = "";
    let message = "";
    let type = "info";

    // WMO Weather Code Mapping for Severe Weather
    if (weather_code >= 95) {
      title = "⛈️ Thunderstorm Warning (IMD Alert)";
      message = "Severe thunderstorm detected in your area. Please stay indoors, avoid taking shelter under trees, and unplug electrical appliances. Stay safe!";
      type = "danger";
    } else if (weather_code === 65 || weather_code === 82 || rain > 10) {
      title = "🌧️ Heavy Rain/Flood Alert (IMD Alert)";
      message = "Extremely heavy rainfall detected. Risk of localized localized flooding and waterlogging. Avoid driving through flooded roads.";
      type = "danger";
    } else if (wind_speed_10m > 50) {
      title = "💨 High Wind Warning (IMD Alert)";
      message = "Strong gale-force winds detected. Stay away from old buildings, trees, and loose power lines. Be cautious!";
      type = "warning";
    }

    // ============================================
    // 2. EARTHQUAKE CHECK (USGS)
    // ============================================
    // Check for earthquakes > 4.0 magnitude within 150km in the last 24 hours
    try {
      const yesterday = new Date(Date.now() - 86400000).toISOString();
      const eqUrl = `https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&latitude=${DEFAULT_LAT}&longitude=${DEFAULT_LON}&maxradiuskm=150\u0026minmagnitude=4.0&starttime=${yesterday}`;
      const eqRes = await axios.get(eqUrl, { 
        timeout: 5000,
        headers: { 'User-Agent': 'SCMS-Disaster-Detection-System' }
      });

      if (eqRes.data && eqRes.data.features && eqRes.data.features.length > 0) {
        const latestQuake = eqRes.data.features[0];
        const mag = latestQuake.properties.mag;
        const place = latestQuake.properties.place;

        // Override weather alert with Earthquake (higher priority)
        title = `🫨 EARTHQUAKE ALERT (Mag ${mag})`;
        message = `A magnitude ${mag} earthquake just occurred near ${place}. Drop, Cover, and Hold On! Check for structural damage and stay away from windows.`;
        type = "danger";
      }
    } catch (eqErr) {
      console.log("⚠️ Earthquake scan failed:", eqErr.message);
    }

    if (title) {
      // 1. Log to server console
      console.log(`📡 Disaster Detected: ${title}`);

      // 2. Check if this specific alert (by title and current date) already exists to avoid spamming
      const today = new Date().toISOString().split('T')[0];
      const existing = await db.collection("alerts")
        .where("title", "==", title)
        .where("area", "==", "System Auto-Detection")
        .get();

      // If we already sent a specific severe alert in the last 4 hours, don't spam
      const fourHoursAgo = Date.now() - (4 * 60 * 60 * 1000);
      const isDuplicate = existing.docs.some(doc => {
        const d = doc.data();
        return d.created_at && (d.created_at.toMillis() > fourHoursAgo);
      });

      if (!isDuplicate) {
        const newId = await getNextId("alerts");
        await db.collection("alerts").doc(newId.toString()).set({
          title,
          message,
          type: type,
          area: "System Auto-Detection",
          reporterName: "SCMS National Disasters Bot",
          created_at: admin.firestore.FieldValue.serverTimestamp()
        });

        // 3. Push Notification to All Users
        if (admin.apps.length > 0) {
          admin.messaging().send({
            topic: "all_users",
            notification: {
              title: title,
              body: message
            },
            data: {
              "screen": "alerts"
            }
          }).catch(e => console.log("FCM Disaster broadcast failed:", e));
        }
      } else {
        console.log(`⏭️ Skipping duplicate alert: ${title}`);
      }
    } else {
      console.log("✅ Conditions normal. No disasters detected.");
    }
  } catch (err) {
    console.error("❌ Disaster checker failed:", err.message);
  }
}

// Start the periodic check
setInterval(checkNaturalDisasters, CHECK_INTERVAL);
// Also run once immediately on startup
setTimeout(checkNaturalDisasters, 5000);

// ================================================
// 📢 DISASTER BROADCAST (IMD STYLE)
// ================================================
app.post("/admin/broadcast-emergency", async (req, res) => {
  const { title, message, type, latitude, longitude } = req.body;
  // type: "flood" | "earthquake" | "tsunami" | "thunderstorm"

  try {
    const alertId = Date.now().toString();
    await db.collection("alerts").doc(alertId).set({
      alertId,
      title: `⚠️ ${title.toUpperCase()}`,
      message,
      type: "danger",
      latitude,
      longitude,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    const usersSnap = await db.collection("users").get();
    const tokens = [];
    usersSnap.forEach(u => {
      if (u.data().fcm_token) tokens.push(u.data().fcm_token);
    });

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        tokens,
        notification: {
          title: `📢 EMERGENCY: ${title}`,
          body: message
        },
        data: { type: "emergency" }
      });
    }

    res.json({ success: true, message: "Emergency broadcast sent to all citizens." });
  } catch (err) {
    res.status(500).json({ success: false });
  }
});

// ===============================
// START SERVER
// ===============================
const os = require("os");
app.listen(3000, "0.0.0.0", () => {
  const interfaces = os.networkInterfaces();
  let wifiIp = "127.0.0.1";
  for (const name in interfaces) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        wifiIp = iface.address;
      }
    }
  }
  console.log("🚀 SCMS Server running on port 3000");
  console.log(`📶 Reachable via Wi-Fi at http://${wifiIp}:3000`);
});
