const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function checkSushanta() {
    console.log("Checking for 'Sushanta'...");
    const snap = await db.collection("users").where("name", ">=", "Sushanta").get();
    if (snap.empty) {
        console.log("No user found with name starting with Sushanta");
    } else {
        snap.forEach(doc => {
            console.log(`User ID: ${doc.id}, Data:`, doc.data());
        });
    }
}

checkSushanta();
