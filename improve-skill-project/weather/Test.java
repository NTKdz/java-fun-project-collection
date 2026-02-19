import java.util.Random;

public class Test {
    static final int INSERTION_SORT_THRESHOLD = 16;
    static final Random RNG = new Random(123);

    public static void main(String[] args) {
        int[] arr = arrayGenerator(100_000_000);
        int[] arr1 = arr.clone();
        int[] temp = new int[arr.length];

        long start = System.nanoTime();
        mergeSort(arr, temp, 0, arr.length - 1);
        long end = System.nanoTime();
        System.out.println("MergeSort: " + (end - start) / 1_000_000 + " ms");

        start = System.nanoTime();
        quickSort(arr1, 0, arr1.length - 1);
        end = System.nanoTime();
        System.out.println("QuickSort: " + (end - start) / 1_000_000 + " ms");
    }

    static int[] arrayGenerator(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++)
            arr[i] = RNG.nextInt();
        return arr;
    }

    /* ================= QUICK SORT ================= */

    static void quickSort(int[] a, int left, int right) {
        while (left < right) {
            if (right - left < INSERTION_SORT_THRESHOLD) {
                insertionSort(a, left, right);
                return;
            }

            int pivotIndex = left + RNG.nextInt(right - left + 1);
            int pivot = a[pivotIndex];
            swap(a, pivotIndex, right);

            int p = partition(a, left, right, pivot);

            // Tail recursion elimination
            if (p - left < right - p) {
                quickSort(a, left, p - 1);
                left = p + 1;
            } else {
                quickSort(a, p + 1, right);
                right = p - 1;
            }
        }
    }

    static int partition(int[] a, int left, int right, int pivot) {
        int i = left;
        for (int j = left; j < right; j++) {
            if (a[j] < pivot)
                swap(a, i++, j);
        }
        swap(a, i, right);
        return i;
    }

    /* ================= MERGE SORT ================= */

    static void mergeSort(int[] a, int[] temp, int left, int right) {
        if (right - left < INSERTION_SORT_THRESHOLD) {
            insertionSort(a, left, right);
            return;
        }

        int mid = (left + right) >>> 1;
        mergeSort(a, temp, left, mid);
        mergeSort(a, temp, mid + 1, right);

        // Already ordered — skip merge
        if (a[mid] <= a[mid + 1]) return;

        merge(a, temp, left, mid, right);
    }

    static void merge(int[] a, int[] temp, int left, int mid, int right) {
        System.arraycopy(a, left, temp, left, right - left + 1);

        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right)
            a[k++] = (temp[i] <= temp[j]) ? temp[i++] : temp[j++];

        while (i <= mid)
            a[k++] = temp[i++];
    }

    /* ================= INSERTION SORT ================= */

    static void insertionSort(int[] a, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            int key = a[i], j = i - 1;
            while (j >= left && a[j] > key)
                a[j + 1] = a[j--];
            a[j + 1] = key;
        }
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }
}
