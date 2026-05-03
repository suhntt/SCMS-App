const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function main() {
  const users = await db.collection("users").get();
  console.log("Total users:", users.size);
  users.forEach(doc => {
    console.log(doc.id, "=>", doc.data());
  });
}
main().catch(console.error);
