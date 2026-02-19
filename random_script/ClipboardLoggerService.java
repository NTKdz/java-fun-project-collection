//import org.h2.tools.Server;
//import java.awt.*;
//import java.awt.datatransfer.*;
//import java.io.File;
//import java.sql.*;
//import java.time.format.DateTimeFormatter;
//
//public class ClipboardLoggerService {
//    private static final String DB_PATH = "./data/clipboard";
//    private static final String DB_URL = "jdbc:h2:file:" + DB_PATH + "/clipboard_history;AUTO_SERVER=TRUE";
//    private static final String DB_USER = "khoi_admin";
//    private static final String DB_PASS = "khoi_pass_1234";
//    private static final int POLL_INTERVAL_MS = 2000; // check every 2 seconds
//
//    public static void main(String[] args) {
//        try {
//            // Ensure folder exists
//            new File(DB_PATH).mkdirs();
//
//            // ✅ Start H2 Web Console
//            Server webServer = Server.createWebServer("-webAllowOthers", "-webPort", "8082").start();
//            System.out.println("🌐 H2 Console running at: " + webServer.getURL());
//            System.out.println("   Connect using JDBC URL: " + DB_URL);
//
//            // ✅ Start clipboard logger
//            runClipboardLogger();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }
//
//    private static void runClipboardLogger() {
//        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
//            setupDatabase(conn);
//
//            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//            PreparedStatement insert = conn.prepareStatement(
//                    "INSERT INTO clipboard_log (content, timestamp) VALUES (?, CURRENT_TIMESTAMP())"
//            );
//
//            String lastText = null;
//            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//            System.out.println("📋 Clipboard Logger started...");
//            while (true) {
//                try {
//                    if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
//                        String current = (String) clipboard.getData(DataFlavor.stringFlavor);
//                        if (current != null && !current.equals(lastText)) {
//                            lastText = current;
//                            insert.setString(1, current);
//                            insert.executeUpdate();
//                            String time = java.time.LocalDateTime.now().format(fmt);
//                            System.out.println("[LOGGED " + time + "] " + shorten(current, 80));
//                        }
//                    }
//                } catch (IllegalStateException ignored) {
//                    // Clipboard temporarily unavailable
//                } catch (Exception e) {
//                    System.err.println("⚠️ Clipboard error: " + e.getMessage());
//                }
//                Thread.sleep(POLL_INTERVAL_MS);
//            }
//        } catch (Exception e) {
//            System.err.println("❌ Logger error: " + e.getMessage());
//        }
//    }
//
//    private static void setupDatabase(Connection conn) throws SQLException {
//        try (Statement st = conn.createStatement()) {
//            st.execute("""
//                CREATE TABLE IF NOT EXISTS clipboard_log (
//                    id IDENTITY PRIMARY KEY,
//                    content CLOB,
//                    timestamp TIMESTAMP
//                )
//            """);
//        }
//    }
//
//    private static String shorten(String text, int maxLength) {
//        if (text == null) return "";
//        text = text.replaceAll("\\s+", " ");
//        if (text.length() <= maxLength) return text;
//        return text.substring(0, maxLength) + "...";
//    }
//}
