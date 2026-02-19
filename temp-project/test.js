const r = 4 / 100 / 365;
let total = 10000;

const month = 15;
const days = 15 * 30;
for (let i = 0; i < days; i++) {
  if(i % 30 === 0 && i !== 0) {
    total += 10000;
  }
  total += total * r;
}

console.log((total + 50000).toFixed(2));