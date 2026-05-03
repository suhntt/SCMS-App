const axios = require('axios');

async function checkWeather() {
  try {
    const lat = 24.8333;
    const lon = 92.7789; // Assam area
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,rain,wind_speed_10m,weather_code`;
    const res = await axios.get(url);
    console.log("Weather Data:", res.data.current);
  } catch (e) {
    console.log("Error:", e.message);
  }
}

checkWeather();
