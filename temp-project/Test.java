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

    private static void bubbleSort(int[] arr, int end) {
        for (int i = 0; i < end; i++) {
            for (int j = 0; j < end - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    public static void countingSort(int[] arr, int start, int end) {
        int min = 0, max = 0;
        for (int i = start; i < end; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        int[] temp = new int[max - min + 1];
        for (int i = start; i < end; i++) {
            temp[arr[i] - min]++;
        }
        int k = 0;
        for (int i = 0; i < temp.length; i++) {
            int value = temp[i];
            while (value > 0) {
                arr[k++] = i + min;
                value--;
            }
        }
//        System.out.println(Arrays.toString(arr));
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
        int[] arr = generateRandomArray(1000000, 0, 100);
        int[] temp1 = arr.clone();
        int[] temp2 = arr.clone();
        int[] temp3 = arr.clone();
        int[] temp5 = arr.clone();
//        System.out.println(Arrays.toString(temp1));
        countingSort(arr, 0, arr.length);
        int[] temp4 = arr.clone();
        for (int i = 0; i < 4; i++) {
            long start = System.currentTimeMillis();
            switch (i) {
                case 0:
                    mergeSort(temp1, temp4, 0, arr.length - 1);
//                    System.out.println(Arrays.toString(temp1));
                    break;
                case 1:
//                    insertionSort(temp2, 0, arr.length - 1);
//                    System.out.println(Arrays.toString(temp2));
                    break;
                case 2:
//                    bubbleSort(temp3, arr.length - 1);
//                    System.out.println(Arrays.toString(temp3));
                    break;
                case 3:
                    countingSort(temp5, 0, arr.length - 1);
                    break;
            }
            long end = System.currentTimeMillis();
            double time = (end - start) / 1000.0;
            System.out.println(time + "s");
        }
    }
}
