const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
}
const db = admin.firestore();

async function testLocally() {
    try {
        const snap = await db.collection("complaints").limit(1).get();
        if (snap.empty) {
            console.log("Empty!"); return;
        }
        
        const comp = snap.docs[0];
        const compId = comp.id;
        const user_id = 12345;
        
        console.log("Testing on complaint:", compId);
    
        const compRef = db.collection("complaints").doc(compId.toString());
        const doc = await compRef.get();
        const row = doc.data();
        const upvoted_by = row.upvoted_by || [];
        
        console.log("Toggle logic upvoted_by", upvoted_by);

        if (upvoted_by.includes(user_id)) {
          await compRef.update({
            upvotes: admin.firestore.FieldValue.increment(-1),
            upvoted_by: admin.firestore.FieldValue.arrayRemove(user_id)
          });
          console.log("success removed")
        } else {
            // New upvote
            await compRef.update({
              upvotes: admin.firestore.FieldValue.increment(1),
              upvoted_by: admin.firestore.FieldValue.arrayUnion(user_id)
            });
            console.log("success added");
        }
    } catch (err) {
        console.log("❌ Catch error locally =>", err);
    }
}

testLocally();
