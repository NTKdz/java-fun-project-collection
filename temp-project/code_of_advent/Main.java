package code_of_advent;

import java.io.*;
import java.util.Arrays;
import java.util.Stack;

public class Main {

    public static void solution3() {
        File file = new File("code_of_advent/input3.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String input;
            int total = 0;
            boolean activated = true;
            while ((input = br.readLine()) != null) {
                System.out.println(input);
                String[] splitInput = input.split("mul\\(");

                for (String s : splitInput) {
                    int num1 = 0;
                    int num2 = 0;
                    int currentNum = 1;
                    boolean isValid = true;
                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        if (!Character.isDigit(c)) {
                            if (c == ',') currentNum++;
                            else if (c == ')') break;
                            else {
                                isValid = false;
                                break;
                            }
                        } else {
                            if (currentNum == 1) num1 = num1 * 10 + (c - '0');
                            else num2 = num2 * 10 + (c - '0');
                        }
                    }
                    int indexDont = s.indexOf("don't()");
                    int indexDo = s.indexOf("do()");
                    if (isValid && activated) total += num1 * num2;

                    if (indexDont == -1 && indexDo == -1) continue;
                    else activated = indexDo > indexDont;

                }
            }
            System.out.println(total);
//            for (int i = 0; i < input.length(); i++) {
//                char ch = input.charAt(i);
//                if (ch == '(') {
//                    isValid = true;
//                } else if (ch == ')') {
//                    isValid = false;
//                }
//                if (Character.isDigit(ch)) {
//
//                }
//            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hello World");
        solution3();
        FileInputStream fstream = new FileInputStream("C:\\testnew\\out.text");
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        BufferedInputStream bin = new BufferedInputStream(fstream);
        String strLine;
        while ((strLine = br.readLine()) != null){
            System.out.println (strLine);
        }
    }
}
