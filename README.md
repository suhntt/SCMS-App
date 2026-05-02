# 🛡️ SCMS: Smart Complaint Management System
### *AI Sentinel Edition — B.Tech CSE Final Year Project*

**SCMS** is a high-performance, full-stack municipal infrastructure platform designed to bridge the gap between citizens and city administration. It leverages **Simulated AI Intelligence** to automate priority scoring, prevent duplicate reporting, and provide real-time decision support for urban governance.

---

## 🚀 Key Features

### 🤖 AI Sentinel Intelligence
- **Visual Fingerprinting:** Detects 90%+ visual similarity in complaint photos to prevent spam and duplicate reports.
- **Semantic Duplication:** Uses Natural Language Processing (NLP) to check for overlapping descriptions in a specific geospatial radius.
- **Priority Scoring (0-100):** Automatically ranks incidents based on urgency keywords (e.g., "Fire", "Live Wire", "Flooding").
- **Sentiment Pulse:** Analyzes the citizen's mood to help admins prioritize distressed reports.

### 📱 Citizen Mobile App (Android)
- **Modern UI:** Built with **Jetpack Compose** and **Material3** with full Dark/Light mode adaptivity.
- **Offline Resilience:** Full support for filing complaints without internet; auto-syncs using **WorkManager** when back online.
- **High-Priority Alerts:** WhatsApp-grade lock-screen notifications for district-wide emergencies.
- **Gamification:** Citizens earn "SCMS Points" for verified reports, encouraging civic engagement.

### 🏛️ Admin Command Center (Web)
- **Tactical Dashboard:** Real-time geospatial mapping of all active incidents.
- **Autonomous Dispatch:** AI suggests the specific municipal unit (PWD, Electricity Board, etc.) for each complaint.
- **Global Broadcast:** Admins can send emergency signals to all citizens in a specific district.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Mobile** | Kotlin, Jetpack Compose, Retrofit2, Room DB, WorkManager |
| **Admin** | React.js, Leaflet Maps, Chart.js, Tailwind CSS |
| **Backend** | Node.js, Express.js, MySQL / Firestore |
| **Cloud** | Firebase Cloud Messaging (FCM), Cloudinary (Image Optimization) |
| **Auth** | Firebase Auth (OTP & Email), JWT-based session management |

---

## 📦 Project Structure

```text
├── app/                  # Android Application (Kotlin/Compose)
├── scms-admin/           # Web Dashboard (React/Vite)
├── scms-backend/          # API Server (Node.js/Express)
├── PROJECT_GUIDE.md      # Detailed Implementation Roadmap
└── README.md             # This file
```

---

## ⚡ Quick Setup

### 1. Backend & Admin
```bash
# Navigate to backend
cd scms-backend
npm install
npm start

# Navigate to admin
cd scms-admin
npm install
npm run dev
```

### 2. Android App
1. Open the `app` folder in **Android Studio (Ladybug or later)**.
2. Ensure you have a valid `google-services.json` in the `app/` directory.
3. Update the `BASE_URL` in `RetrofitClient.kt` to match your local machine's IP.
4. Press **Run**.

---

## 🎓 Academic Context
- **Project Title:** Smart Complaint Management System (SCMS)
- **Level:** B.Tech CSE Final Year Project
- **Key Focus:** Full-Stack Development, AI/ML Integration, Mobile Computing, Urban Governance.

---

## 📜 License
This project is developed for academic purposes. All rights reserved.

---
*Developed with ❤️ for a Smarter Future.*
