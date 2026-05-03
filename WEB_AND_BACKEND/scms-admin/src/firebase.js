import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyAFoFv17EzuNB5OnKvaTczHia0GkZwlAcI",
  authDomain: "scms-17eef.firebaseapp.com",
  projectId: "scms-17eef",
  storageBucket: "scms-17eef.firebasestorage.app",
  messagingSenderId: "477160542514",
  appId: "1:477160542514:web:b009d3f096ffe991e8c72b" // Extrapolated from mobile ID for Firestore access
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
