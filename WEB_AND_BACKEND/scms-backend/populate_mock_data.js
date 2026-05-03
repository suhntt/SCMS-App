const admin = require("firebase-admin");

process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";

admin.initializeApp({
  projectId: "demo-scms-local" // must match the emulator project id
});

const db = admin.firestore();

async function main() {
  console.log("Populating mock data...");

  // Insert mock complaint
  await db.collection("complaints").doc("1001").set({
    id: 1001,
    category: "Road Damage",
    description: "Huge pothole causing accidents.",
    place: "Main Street, Sector 4",
    latitude: "20.5937",
    longitude: "78.9629",
    status: "Pending",
    upvotes: 25,
    severity: "High",
    ai_confidence: 95,
    user_id: "user_123",
    created_at: admin.firestore.FieldValue.serverTimestamp()
  });

  // Insert mock accidents (close to each other to form a blackspot)
  await db.collection("accidents").doc("2001").set({
    accidentId: 2001,
    latitude: "20.5937",
    longitude: "78.9629",
    created_at: admin.firestore.FieldValue.serverTimestamp()
  });

  await db.collection("accidents").doc("2002").set({
    accidentId: 2002,
    latitude: "20.5940",
    longitude: "78.9635",
    created_at: admin.firestore.FieldValue.serverTimestamp()
  });

  console.log("Mock data populated.");
  process.exit(0);
}

main().catch(console.error);
