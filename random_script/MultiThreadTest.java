import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiThreadTest {
    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Hello from a separate thread!");
                    }
                });
            }

            executor.shutdown();
            boolean finished = executor.awaitTermination(5, TimeUnit.MINUTES);

            if (!finished) {
                System.out.println("Not all tasks finished in time!");
                // optional: force shutdown
                executor.shutdownNow();
            }
            long end = System.currentTimeMillis();
            System.out.println("All tasks completed in " + (end - start) + " ms");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}
