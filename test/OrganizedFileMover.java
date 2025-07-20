import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class OrganizedFileMover {
    private static final Map<String, String> extensionToCategory = new HashMap<>();

    static {
        // Images
        addCategory("Images", "png", "jpg", "jpeg", "gif", "bmp", "webp");

        // Videos
        addCategory("Videos", "mp4", "avi", "mov", "wmv", "mkv", "webm");

        // Documents
        addCategory("Documents", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "odt");

        // Archives
        addCategory("Archives", "zip", "rar", "7z", "tar", "gz", "iso");

        // Audio
        addCategory("Audio", "mp3", "wav", "aac", "flac", "ogg");

        // Code
        addCategory("Code", "java", "py", "cpp", "c", "js", "html", "css", "ts", "json", "xml");
    }

    public static void main(String[] args) {
        Path folder = Path.of("C:/Users/ADMIN/Downloads");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path source : stream) {
                if (Files.isDirectory(source)) {
                    continue; // skip folders
                }

                String extension = getExtension(source.getFileName().toString());
                String category = extensionToCategory.getOrDefault(extension, "Others");

                Path targetDir = folder.resolve(category);
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                Path target = targetDir.resolve(source.getFileName());

                // If file exists, add suffix
                target = getUniqueTargetPath(target);

                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Moved " + source.getFileName() + " → " + category + "/");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addCategory(String category, String... extensions) {
        for (String ext : extensions) {
            extensionToCategory.put(ext.toLowerCase(), category);
        }
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1 || dot == filename.length() - 1) ? "" : filename.substring(dot + 1).toLowerCase();
    }

    private static Path getUniqueTargetPath(Path target) {
        int count = 1;
        Path newTarget = target;
        while (Files.exists(newTarget)) {
            String fileName = target.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            String base = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String ext = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

            String newName = base + "(" + count + ")" + ext;
            newTarget = target.getParent().resolve(newName);
            count++;
        }
        return newTarget;
    }
}
