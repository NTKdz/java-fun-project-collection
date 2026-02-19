const fs = require('fs');
const path = require('path');

console.log("=== Node.js Benchmark ===");

// 1. CPU-bound
console.log("\n-- CPU Test --");
function fibIterative(n){
  if (n<=1) return n;
  let a=0,b=1;
  for (let i=2;i<=n;i++){
    [a,b] = [b, a+b];
  }
  return b;
}

let start = process.hrtime.bigint();
let fib = fibIterative(40);
let end = process.hrtime.bigint();
console.log(`Fibonacci(40) = ${fib} | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);

// Sieve of Eratosthenes
start = process.hrtime.bigint();
const n = 1_000_000;
const sieve = Array(n+1).fill(true);
for (let p=2;p*p<=n;p++){
  if (sieve[p]){
    for (let i=p*p;i<=n;i+=p) sieve[i]=false;
  }
}
end = process.hrtime.bigint();
console.log(`Sieve up to ${n} | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);

// 2. Memory-bound
console.log("\n-- Memory Test --");
// Large array sum
const size = 10_000_000;
const arr = Array.from({length:size},(_,i)=>i);
start = process.hrtime.bigint();
let sum = 0;
for (let i=0;i<size;i++) sum += arr[i];
end = process.hrtime.bigint();
console.log(`Sum of ${size} ints: ${sum} | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);

// Large object map
start = process.hrtime.bigint();
const map = {};
for (let i=0;i<1_000_000;i++) map[i] = "value" + i;
end = process.hrtime.bigint();
console.log("Inserted 1M entries in map | Time: " + (Number(end-start)/1e6).toFixed(2) + " ms");

// 3. String manipulation
console.log("\n-- String Test --");
const nChars = 1_000_000;
start = process.hrtime.bigint();
let s = [];
for (let i=0;i<nChars;i++) s.push("a");
let result = s.join('');
end = process.hrtime.bigint();
console.log(`Concatenated ${nChars} chars | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);

// 4. File I/O
console.log("\n-- I/O Test --");
const filename = 'test_io.txt';
const data = 'x'.repeat(100_000_000);
start = process.hrtime.bigint();
fs.writeFileSync(filename, data);
end = process.hrtime.bigint();
console.log(`Write 100MB | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);

start = process.hrtime.bigint();
const readData = fs.readFileSync(filename, 'utf8');
end = process.hrtime.bigint();
console.log(`Read 100MB | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);
fs.unlinkSync(filename);

// 5. Mixed workload
console.log("\n-- Mixed Workload Test --");
const sizeMixed = 1_000_000;
const arrMixed = Array.from({length:sizeMixed},()=>Math.floor(Math.random()*1e6));
start = process.hrtime.bigint();
// Sort
arrMixed.sort((a,b)=>a-b);
// Sum
sum = arrMixed.reduce((a,b)=>a+b,0);
// Write
const filenameMixed = 'mixed.txt';
fs.writeFileSync(filenameMixed, sum.toString());
// Read
const resMixed = fs.readFileSync(filenameMixed,'utf8');
end = process.hrtime.bigint();
console.log(`Mixed workload (sort+sum+write+read) | Time: ${(Number(end-start)/1e6).toFixed(2)} ms`);
fs.unlinkSync(filenameMixed);
