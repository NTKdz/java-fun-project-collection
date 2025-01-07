import time

def sieve_of_eratosthenes(limit):
    primes = [True] * (limit + 1)
    primes[0], primes[1] = False, False
    for i in range(2, int(limit**0.5) + 1):
        if primes[i]:
            for j in range(i * i, limit + 1, i):
                primes[j] = False
    return [i for i, is_prime in enumerate(primes) if is_prime]

def benchmark_prime_computation(limit):
    # Mark the start time before running the computation
    start_time = time.perf_counter()

    # Perform the prime number computation
    sieve_of_eratosthenes(limit)

    # Mark the end time after the computation
    end_time = time.perf_counter()

    # Return the time taken in milliseconds
    return (end_time - start_time) * 1000  # Convert seconds to milliseconds

# Limit for prime computation
limit = 1000000

# Benchmark the prime computation
time_taken_ms = benchmark_prime_computation(limit)
print(f"Prime computation time for limit {limit}: {time_taken_ms:.4f} milliseconds")
