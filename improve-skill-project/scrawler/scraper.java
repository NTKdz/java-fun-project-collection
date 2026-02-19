import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class scraper {
    private static final Random random = new Random();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU cores: " + cores);

        ExecutorService service = Executors.newFixedThreadPool(10);

        List<Callable<Long>> list = IntStream.range(0, 12)
                .<Callable<Long>>mapToObj(x -> new Callable<Long>() {
                    @Override
                    public Long call() {
                        System.out.println("run");
                        return randomNumberTotal();
                    }
                }).toList();

        System.out.println("out run");
//        List<Future<Long>> results = service.invokeAll(list
//        );
        IntStream.range(0, 12)
                .forEach(x -> service.submit(new Callable<Long>() {
                    @Override
                    public Long call() {
                        System.out.println("start");
                        long i =  randomNumberTotal();
                        System.out.println(i);
                        return i;
                    }
                }));

        System.out.println("another out run");

//        results.forEach(x -> {
//            try {
//                System.out.println(x.get());
//            } catch (InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        });

        service.shutdown();
    }

    public static long randomNumberTotal() {
        long total = 0;
        for (int i = 0; i < 10000000; i++) {
            total += random.nextInt();
        }
        return total;
    }
}
