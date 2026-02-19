import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiCounter {

    // UPDATE THESE PATHS
    private static final String DUMP_FILE = "D:\\dataset\\enwiki-latest-pages-articles.xml.bz2";

    public static void main(String[] args) {
        // Fix for large XML entity limits in Java
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        AtomicInteger totalPages = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        System.out.println("Starting count... please wait.");

        try (FileInputStream fis = new FileInputStream(DUMP_FILE);
             BufferedInputStream bis = new BufferedInputStream(fis, 2 * 1024 * 1024); // 2MB buffer
             BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(bis)) {

            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(bzIn, "UTF-8");

            while (reader.hasNext()) {
                int event = reader.next();

                // Every time a <page> tag starts, increment our counter
                if (event == XMLStreamConstants.START_ELEMENT && "page".equals(reader.getLocalName())) {
                    int currentCount = totalPages.incrementAndGet();

                    // Progress update every 100,000 pages so you know it's working
                    if (currentCount % 100000 == 0) {
                        System.out.printf("Counted %,d pages...%n", currentCount);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            double durationMinutes = (endTime - startTime) / 60000.0;

            System.out.println("\n--- FINAL RESULTS ---");
            System.out.printf("Total Pages/Articles: %,d%n", totalPages.get());
            System.out.printf("Time Taken: %.2f minutes%n", durationMinutes);

        } catch (Exception e) {
            System.err.println("Error reading dump: " + e.getMessage());
            e.printStackTrace();
        }
    }
}