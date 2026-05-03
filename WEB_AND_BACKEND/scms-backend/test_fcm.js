const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
}

admin.messaging().send({
    topic: "all_users",
    notification: {
      title: "Test Civic Issue Reported",
      body: "Testing FCM broadcast to all_users"
    }
  }).then(console.log).catch(console.error);
