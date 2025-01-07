import java.util.Arrays;
import java.util.Random;

public class SortingComparison {
    public static void main(String[] args) {
        int arraySize = 1000000; // Size of the array to test
        int[] unsortedArray = generateRandomArray(arraySize, 0, 100);

        System.out.println("Testing sorting algorithms on an array of size: " + arraySize);

        int[] arrayForMergeSort = unsortedArray.clone();
        int[] arrayForQuickSort = unsortedArray.clone();
        int[] arrayForHeapSort = unsortedArray.clone();
        int[] arrayForRadixSort = unsortedArray.clone();
        int[] arrayForCountingSort = unsortedArray.clone();
        int[] arrayForTimSort = unsortedArray.clone();

        // Measure performance
        measurePerformance("Merge Sort", () -> mergeSort(arrayForMergeSort, 0, arrayForMergeSort.length - 1));
        measurePerformance("Quick Sort", () -> quickSort(arrayForQuickSort, 0, arrayForQuickSort.length - 1));
        measurePerformance("Heap Sort", () -> heapSort(arrayForHeapSort));
        measurePerformance("Radix Sort", () -> radixSort(arrayForRadixSort));
        measurePerformance("Counting Sort", () -> countingSort(arrayForCountingSort));
        measurePerformance("Tim Sort (Arrays.sort)", () -> Arrays.sort(arrayForTimSort));
    }

    // Generates an array of random integers
    public static int[] generateRandomArray(int size, int min, int max) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(max - min + 1) + min;
        }
        return array;
    }

    // Measures the execution time of a sorting algorithm
    public static void measurePerformance(String algorithmName, Runnable sortingAlgorithm) {
        long startTime = System.currentTimeMillis();
        sortingAlgorithm.run();
        long endTime = System.currentTimeMillis();
        System.out.println(algorithmName + " took: " + (endTime - startTime) + " ms");
    }

    // Merge Sort Implementation
    public static void mergeSort(int[] arr, int start, int end) {
        if (start >= end) return;
        int mid = (start + end) / 2;
        mergeSort(arr, start, mid);
        mergeSort(arr, mid + 1, end);
        merge(arr, start, mid, end);
    }

    private static void merge(int[] arr, int start, int mid, int end) {
        int[] temp = new int[end - start + 1];
        int i = start, j = mid + 1, k = 0;
        while (i <= mid && j <= end) {
            if (arr[i] <= arr[j]) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }
        while (i <= mid) temp[k++] = arr[i++];
        while (j <= end) temp[k++] = arr[j++];
        System.arraycopy(temp, 0, arr, start, temp.length);
    }

    // Quick Sort Implementation
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);
            quickSort(arr, low, pivotIndex - 1);
            quickSort(arr, pivotIndex + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        int pivotIndex = low + (int) (Math.random() * (high - low + 1));
        swap(arr, pivotIndex, high); // Move random pivot to the end
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // Other Sorting Implementations...
    public static void heapSort(int[] arr) {
        int n = arr.length;

        // Build heap
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

        // Extract elements from heap
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);
            heapify(arr, i, 0);
        }
    }

    private static void heapify(int[] arr, int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && arr[left] > arr[largest]) largest = left;
        if (right < n && arr[right] > arr[largest]) largest = right;

        if (largest != i) {
            swap(arr, i, largest);
            heapify(arr, n, largest);
        }
    }

    public static void radixSort(int[] arr) {
        int max = Arrays.stream(arr).max().getAsInt();
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSortForRadix(arr, exp);
        }
    }

    private static void countingSortForRadix(int[] arr, int exp) {
        int n = arr.length;
        int[] output = new int[n];
        int[] count = new int[10];

        for (int i : arr) count[(i / exp) % 10]++;

        for (int i = 1; i < 10; i++) count[i] += count[i - 1];

        for (int i = n - 1; i >= 0; i--) {
            int index = (arr[i] / exp) % 10;
            output[--count[index]] = arr[i];
        }

        System.arraycopy(output, 0, arr, 0, n);
    }

    public static void countingSort(int[] arr) {
        int max = Arrays.stream(arr).max().getAsInt();
        int min = Arrays.stream(arr).min().getAsInt();
        int range = max - min + 1;

        int[] count = new int[range];
        int[] output = new int[arr.length];

        for (int i : arr) count[i - min]++;

        for (int i = 1; i < range; i++) count[i] += count[i - 1];

        for (int i = arr.length - 1; i >= 0; i--) {
            output[--count[arr[i] - min]] = arr[i];
        }

        System.arraycopy(output, 0, arr, 0, arr.length);
    }
}
