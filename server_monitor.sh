#!/bin/bash

# --- CONFIGURATION ---
# List the ports you want to monitor here (separated by spaces)
# Example: PORTS="5000 5173 3000"
PORTS="5174 5000"
# Time to wait between checks (in seconds)
INTERVAL=10
# ---------------------

echo "🚀 Server Monitor Started"
echo "Watching ports: $PORTS"
echo "Check interval: $INTERVAL seconds"
echo "-----------------------------------"

# First, wait for the servers to be UP if they aren't already
# (Optional: If you want it to shut down immediately if they are off, 
# you can skip this wait part)

echo "Waiting for all specified servers to be UP before starting monitoring..."
for PORT in $PORTS; do
    while ! lsof -i :$PORT -t >/dev/null; do
        echo "Waiting for server on port $PORT..."
        sleep 5
    done
    echo "✅ Server on port $PORT is UP."
done

echo "-----------------------------------"
echo "🛡️  Monitoring active. If any of these servers stop, the laptop will shut down."

while true; do
    for PORT in $PORTS; do
        if ! lsof -i :$PORT -t >/dev/null; then
            echo "🛑 ALERT: Server on port $PORT has turned OFF."
            echo "Shutting down laptop in 5 seconds..."
            sleep 5
            
            # macOS Shutdown command (Clean)
            osascript -e 'tell app "System Events" to shut down'
            
            # Uncomment the line below and comment the one above 
            # if you want an immediate forced shutdown (requires sudo)
            # sudo shutdown -h now
            
            exit 0
        fi
    done
    sleep $INTERVAL
done
