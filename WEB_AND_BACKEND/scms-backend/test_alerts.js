const axios = require('axios');

async function test() {
  try {
    const res = await axios.get('http://localhost:3000/alerts');
    console.log("Success:", res.data);
  } catch(e) {
    if (e.response) console.log("Failed:", e.response.status, e.response.data);
    else console.log("Error:", e.message);
  }
}

test();
