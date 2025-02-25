import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AES {

    // Generate AES Key
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // AES-128
        return keyGenerator.generateKey();
    }

    // Encrypt Text using AES
    public static String encrypt(String plaintext, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes); // Encode as Base64 for easy storage
    }

    // Decrypt AES Encrypted Text
    public static String decrypt(String encryptedText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Read file content
    public static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString().trim();
    }

    // Write encrypted data to file
    public static void writeFile(String filePath, String data) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(data);
        writer.close();
    }

    public static void main(String[] args) {
        try {
            // Generate AES Key
            SecretKey secretKey = generateKey();

            // Read plaintext from file
            String inputFile = "input.txt";  // Provide path to your text file
            String plaintext = readFile(inputFile);
            System.out.println("Original Text: \n" + plaintext);

            // Encrypt the text
            String encryptedText = encrypt(plaintext, secretKey);
            writeFile("encrypted.txt", encryptedText);
            System.out.println("\nEncrypted Text saved to encrypted.txt");

            // Decrypt the text
            String decryptedText = decrypt(encryptedText, secretKey);
            writeFile("decrypted.txt", decryptedText);
            System.out.println("\nDecrypted Text saved to decrypted.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
