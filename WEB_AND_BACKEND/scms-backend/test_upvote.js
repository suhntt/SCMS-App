const axios = require('axios');

async function test() {
  try {
    const res = await axios.post('http://localhost:3000/upvote/12', { user_id: 12345 });
    console.log("Upvote 1:", res.data);
    
    const res2 = await axios.post('http://localhost:3000/upvote/12', { user_id: 12345 });
    console.log("Upvote 2:", res2.data);
  } catch(e) {
    if (e.response) console.log(e.response.data);
    else console.log(e.message);
  }
}

test();
