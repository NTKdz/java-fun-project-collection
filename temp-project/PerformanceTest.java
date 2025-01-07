import java.util.*;

public class PerformanceTest {
    public static List<Integer> sieveOfEratosthenes(int limit) {
        boolean[] primes = new boolean[limit + 1];
        Arrays.fill(primes, true);
        primes[0] = primes[1] = false;

        for (int i = 2; i * i <= limit; i++) {
            if (primes[i]) {
                for (int j = i * i; j <= limit; j += i) {
                    primes[j] = false;
                }
            }
        }

        List<Integer> primeNumbers = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (primes[i]) {
                primeNumbers.add(i);
            }
        }
        return primeNumbers;
    }

    public static void main(String[] args) {
        int limit = 1000000;
        long startTime = System.nanoTime();
        List<Integer> primes = sieveOfEratosthenes(limit);
        long endTime = System.nanoTime();
        double timeTaken = (endTime - startTime) / 1e6;
        System.out.println("Prime computation time: " + timeTaken + " ms");
    }
}
