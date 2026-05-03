#!/bin/bash

echo "🚀 INITIALIZING SCMS MISSION CONTROL..."
echo "--------------------------------------"

# ── Kill any leftover ghost processes from previous runs ──
echo "🧹 Clearing old processes..."
lsof -ti :3000 | xargs kill -9 2>/dev/null && echo "   ↳ Cleared port 3000" || true
lsof -ti :5173 | xargs kill -9 2>/dev/null && echo "   ↳ Cleared port 5173" || true
sleep 1

# ── Auto-install backend deps if missing ──
if [ ! -d "WEB_AND_BACKEND/scms-backend/node_modules" ]; then
  echo "📦 Installing backend dependencies..."
  (cd WEB_AND_BACKEND/scms-backend && npm install --silent)
fi

# ── Auto-install admin deps if missing ──
if [ ! -d "WEB_AND_BACKEND/scms-admin/node_modules" ]; then
  echo "📦 Installing admin dependencies..."
  (cd WEB_AND_BACKEND/scms-admin && npm install --silent)
fi

# ── Start Backend ──
echo "📡 Starting Backend Intelligence Center..."
(cd "WEB_AND_BACKEND/scms-backend" && npm start) &

# Wait for backend to initialize
sleep 2

# ── Start Admin Dashboard ──
echo "🏛️ Launching Admin Mission Control (Dev Mode)..."
(cd "WEB_AND_BACKEND/scms-admin" && npm run dev)

# Note: Closing this terminal stops both processes.
