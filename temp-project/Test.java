import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.Random;

public class Test {
    public static void mergeSort(int[] arr, int[] temp, int start, int end) {
        if (end - start <= 32) {
            insertionSort(arr, start, end); // Switch to insertion sort for small arrays
            return;
        }

        int mid = (start + end) / 2;

        mergeSort(arr, temp, start, mid);
        mergeSort(arr, temp, mid + 1, end);

        merge(arr, temp, start, mid, end);
    }

    private static void merge(int[] arr, int[] temp, int start, int mid, int end) {
        System.arraycopy(arr, start, temp, start, end - start + 1);

        int i = start, j = mid + 1, k = start;

        while (i <= mid && j <= end) {
            if (temp[i] <= temp[j]) {
                arr[k++] = temp[i++];
            } else {
                arr[k++] = temp[j++];
            }
        }

        while (i <= mid) {
            arr[k++] = temp[i++];
        }

        while (j <= end) {
            arr[k++] = temp[j++];
        }
    }

    private static void insertionSort(int[] arr, int start, int end) {
        for (int i = start + 1; i <= end; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= start && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    public static int[] generateRandomArray(int size, int min, int max) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(max - min + 1) + min;
        }
        return array;
    }

    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().build();
        client.close();
    }
}
