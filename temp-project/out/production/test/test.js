function sieveOfEratosthenes(limit) {
    let primes = Array(limit + 1).fill(true);
    primes[0] = primes[1] = false;

    for (let i = 2; i * i <= limit; i++) {
        if (primes[i]) {
            for (let j = i * i; j <= limit; j += i) {
                primes[j] = false;
            }
        }
    }

    let primeNumbers = [];
    for (let i = 2; i <= limit; i++) {
        if (primes[i]) {
            primeNumbers.push(i);
        }
    }
    return primeNumbers;
}

let limit = 1000000;
let startTime = process.hrtime();
let primes = sieveOfEratosthenes(limit);
let endTime = process.hrtime(startTime);
let timeTaken = endTime[0] * 1000 + endTime[1] / 1000000;
console.log(`Prime computation time: ${timeTaken.toFixed(4)} ms`);
