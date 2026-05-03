const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

// Connect to LIVE Firestore
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function main() {
  console.log("🚀 Uploading mock data to LIVE Firestore...");

  // 1. Mock Complaint
  await db.collection("complaints").doc("1001").set({
    id: 1001,
    category: "Road Damage",
    title: "Big Pothole",
    description: "Huge pothole causing accidents near the main crossing.",
    place: "Sector 5, Kolkata",
    latitude: "22.5726",
    longitude: "88.3639",
    status: "Pending",
    upvotes: 12,
    severity: "High",
    ai_confidence: 0.98,
    created_at: admin.firestore.FieldValue.serverTimestamp()
  });

  // 2. Mock Alert
  await db.collection("alerts").doc("1").set({
    id: 1,
    title: "Water Leakage Alert",
    message: "Major pipe burst reported in Sector 2. Expect low pressure.",
    type: "warning",
    area: "Sector 2",
    created_at: admin.firestore.FieldValue.serverTimestamp()
  });

  // 3. Mock Leaderboard
  const users = [
    { id: 1, name: "Sushanta", points: 450, badgeLevel: "Hero" },
    { id: 2, name: "Rahul", points: 320, badgeLevel: "Guardian" },
    { id: 3, name: "Ananya", points: 150, badgeLevel: "Citizen" }
  ];

  for (const user of users) {
    await db.collection("users").doc(user.id.toString()).set(user);
  }

  console.log("✅ LIVE Mock data uploaded successfully!");
  process.exit(0);
}

main().catch(console.error);
