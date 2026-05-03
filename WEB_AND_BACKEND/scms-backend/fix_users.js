const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fix() {
  const usersRef = db.collection("users");
  const snap = await usersRef.get();
  
  let batch = db.batch();
  snap.forEach(doc => {
    let data = doc.data();
    if (!data.id) {
       console.log("Fixing doc:", doc.id);
       let generatedId = Math.floor(Math.random() * 2147483647);
       batch.update(doc.ref, {
         id: generatedId,
         points: 0,
         badgeLevel: "Citizen"
       });
    }
  });
  await batch.commit();
  console.log("✅ Fixed all existing users!");
}
fix().catch(console.error);
