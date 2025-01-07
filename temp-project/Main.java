import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<List<Integer>> reports = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("data.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split("\\s+"); // Split by spaces or tabs
                List<Integer> row = new ArrayList<>();
                for (String column : columns) {
                    row.add(Integer.parseInt(column));
                }
                reports.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int safeCount = 0;

        for (List<Integer> report : reports) {
            if (isSafe(report)) {
                safeCount++;
            } else {
                boolean safeAfterRemoval = false;
                for (int i = 0; i < report.size(); i++) {
                    List<Integer> modifiedReport = new ArrayList<>(report);
                    modifiedReport.remove(i);
                    if (isSafe(modifiedReport)) {
                        safeAfterRemoval = true;
                        break;
                    }
                }
                if (safeAfterRemoval) {
                    safeCount++;
                }
            }
        }

        System.out.println("Number of safe reports: " + safeCount);
    }

    private static boolean isSafe(List<Integer> report) {
        if (report.size() < 2) return true;

        boolean increasing = report.get(1) > report.get(0);
        for (int i = 1; i < report.size(); i++) {
            int diff = report.get(i) - report.get(i - 1);

            if (Math.abs(diff) < 1 || Math.abs(diff) > 3) {
                return false;
            }
            
            if ((diff > 0 && !increasing) || (diff < 0 && increasing)) {
                return false;
            }
        }
        return true;
    }
}
