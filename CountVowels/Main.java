public class Main {
    String test="";
    public static void main(String[] args) {
        System.out.println("Hello World");
        countVowels("hello world");
    }

    public static void countVowels(String word) {
        int[] countVowels = new int[6];
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            switch (ch) {
                case 'a':
                    countVowels[0]++;
                    break;
                case 'e':
                    countVowels[1]++;
                    break;
                case 'i':
                    countVowels[2]++;
                    break;
                case 'o':
                    countVowels[3]++;
                    break;
                case 'u':
                    countVowels[4]++;
                    break;
            }
        }

        for (int i = 0; i < countVowels.length; i++) {
            System.out.println(countVowels[i]);
            countVowels[5] += countVowels[i];
        }

        System.out.println(countVowels[5]);
    }
}