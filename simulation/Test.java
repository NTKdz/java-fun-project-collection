import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int index = low - 1;
        for (int i = low; i < high; i++) {
            if (arr[i] < pivot) {
                index++;
                int temp = arr[index];
                arr[index] = arr[i];
                arr[i] = temp;
            }
        }
        index++;
        arr[high] = arr[index];
        arr[index] = pivot;
        return index;
    }

    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pivot = partition(arr, low, high);
            quickSort(arr, low, pivot - 1);
            quickSort(arr, pivot + 1, high);
        }
    }

    public static void main(String[] args) {
        String filename = "data.txt";
        List<Integer> arrList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Read the first line
            if (line != null) {
                // Remove square brackets and split by commas
                line = line.replaceAll("[\\[\\]]", "").trim();
                String[] numbers = line.split("\\s*,\\s*");

                // Convert to integers and store in the list
                for (String num : numbers) {
                    arrList.add(Integer.parseInt(num));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert list to an array
        int[] arr = arrList.stream().mapToInt(Integer::intValue).toArray();
        long start = System.currentTimeMillis();
//        quickSort(arr, 0, arr.length - 1);
        Arrays.sort(arr);
        long end = System.currentTimeMillis();
        // Print the array to verify
        System.out.println(Arrays.toString(arr));
        System.out.println(end - start);
    }
}