package code_of_advent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

public class Day4 {
    public static void countCurrent(int row, int col, String block) {
        //check horizontal, vertical and reverse
        StringBuilder current = new StringBuilder();
        for (int i = row; i < row + 4; i++) {
            current.append("*");
        }
        //check diagonal and reverse
    }

    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new FileReader("code_of_advent/input4.txt"))) {
            String strLine;
            StringBuilder currentBlock = new StringBuilder();
            int index = 0;
            while ((strLine = br.readLine()) != null) {
                index++;
                currentBlock.append(strLine).append("\n");
                if (index % 4 == 0) {
                    System.out.println(currentBlock);
                    System.out.println();
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 140; j++) {

                        }
                    }
                    countCurrent(index, index, strLine);
                    currentBlock = new StringBuilder();
                    index = 0;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
