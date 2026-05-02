# 🚀 SCMS: Smart Complaint Management System
## Project Structure Guide

This project has been reorganized for maximum performance and clarity. All redundant duplicates and backup files have been moved to the `ARCHIVE_CLEANUP` folder to prevent the IDE from slowing down.

### 📁 Core Folders

- **`/app`**: The Android application. Built with **Kotlin** and **Jetpack Compose**. This is the mobile interface for users to file complaints.
- **`/scms-admin`**: The Web Dashboard. Built with **React** and **Vite**. This is where administrators manage and resolve complaints.
- **`/scms-backend`**: The Logic Center. Built with **Node.js** and **Express**. It handles the database (MySQL/Firebase) and API requests.
- **`/gradle`**: System files required to build the Android app.

### 🛠️ Execution Scripts

- **`start_scms.sh`**: The "Mission Control" script. Run this to start both the Backend and the Admin Dashboard simultaneously.
- **`server_monitor.sh`**: A security tool that monitors your local servers and automatically shuts down the system if they go offline.

### 📝 How to Code in This Project

#### 1. Working on the Mobile App
Open the root directory in **Android Studio**. Navigate to `app/src/main/java` to edit the UI and Logic.

#### 2. Working on the Admin Dashboard
Open the `/scms-admin` folder. 
- Use `npm run dev` to start the dashboard.
- Edit components inside `scms-admin/src`.

#### 3. Working on the Backend
Open the `/scms-backend` folder.
- Edit `server.js` or the routes inside the backend folder.
- Ensure your database (MySQL/Firestore) is running.

---

### ⚠️ Performance Fix
I have removed the duplicate `SCMS` folder which was causing your IDE to scan files twice. Your indexing should now be much faster. 

**Status:** ✅ Properly Structured & Optimized.
