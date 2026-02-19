import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ImageCompressor {

    public static void compressImage(String inputPath, String outputPath, float quality) throws IOException {
        File input = new File(inputPath);
        BufferedImage image = ImageIO.read(input);

        File output = new File(outputPath);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(output);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 0.0 - 1.0

        writer.write(null, new IIOImage(image, null, null), param);

        ios.close();
        writer.dispose();
    }

    public static void main(String[] args) throws IOException {
        compressImage("img.png", "compressed.jpg", 0.00001f);
    }
}
