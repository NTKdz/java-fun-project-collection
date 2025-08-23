import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class FileOrganizer {
    private Map<String, String> fileTypeMap;

    private enum FileType {
        TEXT_DOCUMENT, IMAGE, VIDEO, ARCHIVE, UNKNOWN
    }

    public FileOrganizer() {
        fileTypeMap = Map.of(
                ".txt", "Text Document",
                ".png", "Image",
                ".jpg", "Image",
                ".jpeg", "Image",
                ".mp4", "Video",
                ".avi", "Video",
                ".zip", "Archive",
                ".rar", "Archive"
        );
    }

    public static void main(String[] args) throws IOException {
        // Example usage
        String[] filee = {"document.txt", "image.png", "video.mp4", "archive.zip"};
        String[] validFileType = {"Text Document", "Image", "Video", "Archive"};
        organizeFiles(filee);
        Path path = Path.of("./");
        System.out.printf("Files in directory '%s':%n", path.toAbsolutePath());
        try (Stream<Path> files = Files.walk(path)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        System.out.println(file.getFileName());
                        String fileType = getFileType(file.getFileName().toString());
                        System.out.println("Type: " + fileType);
                        try {
                            Files.move(file, path.resolve(file.getFileName()),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void organizeFiles(String[] files) {
        for (String file : files) {
            String fileType = getFileType(file);
            System.out.println("Organizing " + file + " as a " + fileType);
            // Here you would add logic to move the file to the appropriate directory
        }
    }

    private static String getFileType(String fileName) {
        if (fileName.endsWith(".txt")) {
            return "Text Document";
        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "Image";
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) {
            return "Video";
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
            return "Archive";
        } else {
            return "Unknown Type";
        }
    }
}
